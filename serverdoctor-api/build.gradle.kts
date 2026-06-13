dependencies {
    api(project(":serverdoctor-common"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}
