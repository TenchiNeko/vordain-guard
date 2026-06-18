plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:crypto"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
