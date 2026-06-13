dependencies {
    implementation(project(":serverdoctor-core"))
    implementation(project(":serverdoctor-api"))
    implementation(project(":serverdoctor-common"))
    implementation(project(":serverdoctor-storage"))

    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)   // generiert velocity-plugin.json aus @Plugin
}
