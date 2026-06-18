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
    implementation(project(":vpn:lifecycle"))
    implementation(project(":vpn:session"))
    implementation(project(":vpn:engine"))
    testImplementation(project(":core:events"))
    testImplementation(project(":data:local"))
    testImplementation(kotlin("test"))
}
