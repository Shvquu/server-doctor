plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":serverdoctor-core"))
    implementation(project(":serverdoctor-api"))
    implementation(project(":serverdoctor-common"))

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

    shadowJar {
        archiveBaseName.set("ServerDoctor")
        archiveClassifier.set("")
        // Bundelt common/api/core in die finale Plugin-Jar.
    }

    build {
        dependsOn(shadowJar)
    }
}
