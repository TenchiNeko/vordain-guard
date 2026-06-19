plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:policy"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
