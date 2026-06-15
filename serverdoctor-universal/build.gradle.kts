plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    // Beide Plattform-Module; ihre Plattform-APIs sind compileOnly und werden NICHT gebündelt.
    implementation(project(":serverdoctor-paper"))
    implementation(project(":serverdoctor-velocity"))
    // core/storage/api/common + sqlite-jdbc kommen transitiv und werden gebündelt.
}

tasks {
    shadowJar {
        archiveBaseName.set("serverdoctor")
        archiveClassifier.set("")
        relocate("org.yaml.snakeyaml", "com.serverdoctor.libs.snakeyaml")
    }
    build { dependsOn(shadowJar)
    }
}
