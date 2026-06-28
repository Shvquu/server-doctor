rootProject.name = "serverdoctor"

include(
    "serverdoctor-common",
    "serverdoctor-api",
    "serverdoctor-storage",
    "serverdoctor-core",
    "serverdoctor-paper",
    "serverdoctor-testing",
    "serverdoctor-universal",
    "serverdoctor-rest-api",
    "serverdoctor-velocity",
    "serverdoctor-webhook",
    "serverdoctor-bungeecord",
    "serverdoctor-example-plugin"
)

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")

        // BungeeCord API
        maven("https://oss.sonatype.org/content/repositories/snapshots") {
            mavenContent { snapshotsOnly() }   // optional: nur Snapshots hier suchen
        }
    }
}