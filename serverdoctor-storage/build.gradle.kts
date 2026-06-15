dependencies {
    api(project(":serverdoctor-api"))
    implementation(project(":serverdoctor-common"))
    implementation(libs.sqlite.jdbc)

    implementation(libs.hikaricp)
    implementation(libs.postgresql)
    implementation(libs.mariadb.client)
    implementation(libs.mongodb.sync)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}
