dependencies {
    implementation(project(":serverdoctor-core"))
    implementation(project(":serverdoctor-api"))
    implementation(project(":serverdoctor-common"))
    implementation(project(":serverdoctor-storage"))
    implementation(project(":serverdoctor-rest-api"))
    implementation(project(":serverdoctor-webhook"))

    compileOnly(libs.paper.api)
    compileOnly(libs.placeholderapi)   // optional - nur zur Compile-Zeit
}

tasks {
    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}