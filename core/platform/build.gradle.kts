plugins { id("launcher.android-library") }
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(libs.coroutines)
    implementation(libs.coil)
    implementation(libs.activity)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
