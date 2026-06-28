dependencies {
    // ServerDoctor itself must be installed on the server at runtime; we only need its
    // public API (and the shared models) at compile time -> compileOnly.
    compileOnly(project(":serverdoctor-api"))
    compileOnly(project(":serverdoctor-common"))

    compileOnly(libs.paper.api)
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
