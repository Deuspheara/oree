plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply
        false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
}

spotless {
    kotlin {
        target(
            fileTree("app/src") {
                include("**/*.kt")
            },
            fileTree("benchmark/src") {
                include("**/*.kt")
            },
            fileTree("core") { include("*/src/**/*.kt") },
            fileTree("feature") { include("*/src/**/*.kt") },
        )
        ktlint(
            libs.versions.ktlint.get(),
        ).setEditorConfigPath(
            rootProject.file(".editorconfig"),
        ).editorConfigOverride(
            mapOf(
                "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                "ktlint_standard_no-unused-imports" to "enabled",
            ),
        )
    }
    kotlinGradle {
        target(
            files(
                "build.gradle.kts",
                "settings.gradle.kts",
                "app/build.gradle.kts",
                "benchmark/build.gradle.kts",
                "build-logic/build.gradle.kts",
                "build-logic/settings.gradle.kts",
            ),
            fileTree("core") {
                include("*/build.gradle.kts")
            },
            fileTree("feature") { include("*/build.gradle.kts") },
        )
        ktlint(
            libs.versions.ktlint.get(),
        ).setEditorConfigPath(
            rootProject.file(".editorconfig"),
        ).editorConfigOverride(
            mapOf(
                "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                "ktlint_standard_no-unused-imports" to "enabled",
            ),
        )
    }
}

// Architecture is a build contract, rather than a convention enforced only in review.
tasks.register("verifyArchitecture") {
    group = "verification"
    description = "Checks module direction, package layout, and Android-free domain/ViewModel boundaries."
    doLast {
        val shared = setOf(":core:model", ":core:domain")
        val featureDependencies = shared + setOf(":core:platform", ":core:designsystem")
        val allowed = mapOf(
            ":core:model" to emptySet(),
            ":core:domain" to setOf(":core:model"),
            ":core:data" to shared,
            ":core:platform" to shared,
            ":core:designsystem" to shared,
        )
        subprojects.forEach { module ->
            val permitted = allowed[module.path] ?: if (module.path.startsWith(":feature:")) featureDependencies else null
            if (permitted != null) {
                module.configurations.filter {
                    it.name in setOf("api", "implementation", "compileOnly", "runtimeOnly")
                }.forEach { configuration ->
                    configuration.dependencies.withType<org.gradle.api.artifacts.ProjectDependency>().forEach { dependency ->
                        check(dependency.path in permitted) { "${module.path} cannot depend on ${dependency.path}" }
                    }
                }
            }
            module.fileTree("src/main").matching { include("**/*.kt") }.files.forEach { source ->
                val code = source.readText()
                val packageName = Regex("(?m)^package ([\\w.]+)").find(code)?.groupValues?.get(1) ?: error("Missing package: $source")
                check(source.invariantSeparatorsPath.endsWith(packageName.replace('.', '/') + "/" + source.name)) {
                    "Source directory does not match package: $source"
                }
                val imports = Regex("(?m)^import ([\\w.]+)").findAll(code).map { it.groupValues[1] }.toList()
                val portable = module.path in setOf(":core:model", ":core:domain")
                val viewModel = source.name.endsWith("ViewModel.kt")
                if (portable || viewModel) {
                    imports.forEach { dependency ->
                        check(
                            !dependency.startsWith("android.") && !dependency.startsWith("fr.deuspheara.oree.core.data.") &&
                                !dependency.startsWith("fr.deuspheara.oree.core.platform."),
                        ) {
                            "Implementation or Android dependency in portable code: $source imports $dependency"
                        }
                        if (portable) {
                            check(!dependency.startsWith("androidx.") && !dependency.startsWith("org.koin.")) {
                                "Framework dependency in $source"
                            }
                        }
                    }
                }
                if (module.path.startsWith(":feature:")) {
                    imports.forEach { dependency ->
                        check(!dependency.contains(".core.platform.image.impl.") && !dependency.contains(".core.platform.datasource.")) {
                            "Platform implementation in feature: $source imports $dependency"
                        }
                    }
                    val ownFeature = "fr.deuspheara.oree.feature.${module.name}."
                    imports.filter { it.startsWith("fr.deuspheara.oree.feature.") }.forEach { dependency ->
                        check(dependency.startsWith(ownFeature)) { "Cross-feature dependency: $source imports $dependency" }
                    }
                }
            }
        }
    }
}
