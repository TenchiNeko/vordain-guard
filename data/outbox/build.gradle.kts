plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:events"))
    implementation(project(":core:crypto"))
    implementation(project(":data:local"))
    implementation(project(":data:relay"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
