plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:events"))
    implementation(project(":core:crypto"))
    implementation(project(":data:relay"))
}
