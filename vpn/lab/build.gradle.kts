plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:intelligence"))
    implementation(project(":core:policy"))
    implementation(project(":vpn:packet"))
    implementation(project(":vpn:dns"))
    implementation(project(":vpn:classifier"))
    implementation(project(":vpn:engine"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
