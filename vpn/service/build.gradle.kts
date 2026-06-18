plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.vordain.guard.vpn.service"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    implementation(project(":vpn:engine"))
}
