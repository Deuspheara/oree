plugins { id("launcher.jvm") }
dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
