plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.vordain.guard.child"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vordain.guard.child"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":features:child-status"))
    implementation(project(":features:setup-checklist"))
    implementation(project(":data:local"))
    implementation(project(":data:relay"))
    implementation(project(":vpn:service"))
}
