dependencies {
    implementation(project(":serverdoctor-core"))
    implementation(project(":serverdoctor-api"))
    implementation(project(":serverdoctor-common"))
    implementation(project(":serverdoctor-storage"))
    implementation(project(":serverdoctor-rest-api"))
    implementation(project(":serverdoctor-webhook"))

    implementation(libs.snakeyaml)            // nur für das config.yml-Parsing
    implementation(libs.bstats.velocity)

    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)
}
