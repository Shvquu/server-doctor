dependencies {
    api(project(":serverdoctor-api"))
    implementation(project(":serverdoctor-common"))
    implementation(libs.snakeyaml)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(project(":serverdoctor-testing"))
}
