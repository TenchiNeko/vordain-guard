plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:events"))
    implementation(project(":data:local"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
