plugins {
    `maven-publish`
}

dependencies {
    api(project(":serverdoctor-common"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            // artifactId = serverdoctor-api (Projektname)
        }
    }
    repositories {
        // Nur für GitHub Packages benötigt; JitPack nutzt mavenLocal automatisch.
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Shvquu/server-doctor")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
