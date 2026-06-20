plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:sync-bundle"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
