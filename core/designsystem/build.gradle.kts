plugins { id("launcher.compose") }
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(libs.coroutines)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.tv.material)
    implementation(libs.activity)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.coil)
    implementation(libs.haze.glass)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
