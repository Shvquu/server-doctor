dependencies {
    api(project(":serverdoctor-api"))
    implementation(project(":serverdoctor-common"))
    implementation(libs.sqlite.jdbc)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}
