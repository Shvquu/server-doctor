dependencies {
    // Fixtures (FakeServerPlatform) brauchen die Plattform-Interfaces aus dem Core.
    implementation(project(":serverdoctor-core"))
    implementation(project(":serverdoctor-api"))
    implementation(project(":serverdoctor-common"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.archunit.junit5)
    // Damit ArchUnit auch die Storage-Klassen scannt:
    testImplementation(project(":serverdoctor-storage"))
}
