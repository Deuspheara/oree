plugins { id("launcher.jvm") }
dependencies {
    implementation(project(":core:model"))
    implementation(libs.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
