plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:policy"))
    implementation(project(":vpn:classifier"))
    implementation(project(":data:relay"))
}
