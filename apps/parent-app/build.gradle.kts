plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.vordain.guard.parent"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vordain.guard.parent"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(project(":features:pairing"))
    implementation(project(":features:parent-dashboard"))
    implementation(project(":features:policy-editor"))
    implementation(project(":features:alerts"))
    implementation(project(":features:setup-checklist"))
    implementation(project(":data:local"))
    implementation(project(":data:relay"))
}
