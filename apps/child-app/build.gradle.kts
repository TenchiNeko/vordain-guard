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
    implementation(project(":core:intelligence"))
    implementation(project(":core:pairing"))
    implementation(project(":core:policy"))
    implementation(project(":core:policy-sync"))
    implementation(project(":core:status-report"))
    implementation(project(":features:child-status"))
    implementation(project(":features:setup-checklist"))
    implementation(project(":features:bypass-risk"))
    implementation(project(":data:local"))
    implementation(project(":data:review"))
    implementation(project(":data:relay"))
    implementation(project(":vpn:classifier"))
    implementation(project(":vpn:engine"))
    implementation(project(":vpn:packet"))
    implementation(project(":vpn:lab"))
    implementation(project(":vpn:service"))
}
