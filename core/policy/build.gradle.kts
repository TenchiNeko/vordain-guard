plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:model"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
