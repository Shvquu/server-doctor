dependencies {
    implementation(project(":serverdoctor-core"))
    implementation(project(":serverdoctor-api"))
    implementation(project(":serverdoctor-common"))
    implementation(project(":serverdoctor-storage"))

    implementation(libs.snakeyaml)            // nur für das config.yml-Parsing

    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)
}
