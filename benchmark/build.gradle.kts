plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.baselineprofile)
}
android {
    namespace = "fr.deuspheara.oree.benchmark"
    compileSdk = 37
    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "fr.deuspheara.oree.benchmark.LauncherBenchmarkRunner"
    }
    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
baselineProfile { useConnectedDevices = true }
dependencies {
    implementation(libs.benchmark.macro)
    implementation(libs.androidx.test)
    implementation(libs.runner)
    implementation(libs.uiautomator)
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
