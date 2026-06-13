rootProject.name = "serverdoctor"

include(
    "serverdoctor-common",
    "serverdoctor-api",
    "serverdoctor-storage",
    "serverdoctor-core",
    "serverdoctor-paper",
)

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}
