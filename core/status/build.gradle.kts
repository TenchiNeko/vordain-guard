plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:events"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
