dependencies {
    api(project(":serverdoctor-api"))
    implementation(project(":serverdoctor-common"))

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
