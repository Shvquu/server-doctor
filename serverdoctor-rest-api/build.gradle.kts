plugins {
    `java-library`
}

dependencies {
    implementation(project(":serverdoctor-api"))
    implementation(project(":serverdoctor-common"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}
