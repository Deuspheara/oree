import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.baselineprofile)
}
val localSigningEnabled = providers.gradleProperty("oree.localSigning").map(String::toBoolean).getOrElse(false)
val localSigningProperties = Properties().apply {
    if (localSigningEnabled) {
        val propertiesFile = rootProject.file(".signing/release.properties")
        require(propertiesFile.isFile) { "Run python3 scripts/setup-release-signing.py first" }
        propertiesFile.inputStream().use(::load)
    }
}
android {
    namespace = "fr.deuspheara.oree"
    compileSdk = 37
    defaultConfig {
        applicationId = "fr.deuspheara.oree"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.0-dev"
        testInstrumentationRunner = "fr.deuspheara.oree.LauncherTestRunner"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    signingConfigs {
        if (localSigningEnabled) {
            create("localRelease") {
                storeFile = rootProject.file(".signing/oree-release.p12")
                storePassword = requireNotNull(localSigningProperties.getProperty("storePassword"))
                keyAlias = requireNotNull(localSigningProperties.getProperty("keyAlias"))
                keyPassword = requireNotNull(localSigningProperties.getProperty("keyPassword"))
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (localSigningEnabled) signingConfig = signingConfigs.getByName("localRelease")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions { unitTests.isReturnDefaultValues = true }
    lint { abortOnError = true }
}
composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose-reports")
    metricsDestination = layout.buildDirectory.dir("compose-metrics")
}
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:platform"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:home"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:profiles"))
    implementation(project(":feature:search"))
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.viewmodel)
    testImplementation(libs.koin.test)
    testImplementation(libs.coroutines.test)

    implementation(libs.haze.glass)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.tv.material)
    implementation(libs.compose.tracing)
    implementation(libs.profileinstaller)
    baselineProfile(project(":benchmark"))
    implementation(libs.activity)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.coroutines)
    implementation(libs.datastore)
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    debugImplementation(libs.compose.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.androidx.test)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.coroutines.test)
    debugImplementation(libs.compose.test.manifest)
}

baselineProfile {
    automaticGenerationDuringBuild = false
    saveInSrc = true
    mergeIntoMain = true
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
