import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
    application
}

group = "tech.ghenkle"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    //kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")

    //networking
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    //json
    implementation("com.beust:klaxon:5.6")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("tech.ghenkle.kumquatt.MainKt")
}