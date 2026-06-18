plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:policy"))
    implementation(project(":vpn:classifier"))
    implementation(project(":vpn:dns"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
