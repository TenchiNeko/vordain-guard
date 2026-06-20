plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:dev-relay"))
    implementation(project(":core:model"))
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.vordain.guard.backend.relayapi.DevRelayServerMainKt")
}

tasks.test {
    useJUnitPlatform()
}
