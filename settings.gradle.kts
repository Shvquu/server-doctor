rootProject.name = "serverdoctor"

include(
    "serverdoctor-common",
    "serverdoctor-api",
    "serverdoctor-storage",
    "serverdoctor-core",
    "serverdoctor-paper",
    "serverdoctor-testing",
    "serverdoctor-universal",
    "serverdoctor-velocity",
)

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}
