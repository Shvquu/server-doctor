plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    // All platform modules; their platform APIs are compileOnly and are NOT bundled.
    implementation(project(":serverdoctor-paper"))
    implementation(project(":serverdoctor-velocity"))
    implementation(project(":serverdoctor-bungeecord"))
    // core/storage/api/common + sqlite-jdbc + bstats come transitively and are bundled.
}

tasks {
    shadowJar {
        archiveBaseName.set("serverdoctor")
        archiveClassifier.set("")

        // bStats refuses to start unless its package is relocated out of org.bstats.
        relocate("org.bstats", "com.serverdoctor.libs.bstats")
    }
    build {
        dependsOn(shadowJar)
    }
}