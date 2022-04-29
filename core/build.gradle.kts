import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins{
    `java-library`
}

dependencies {
    //kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")

    //networking
    api("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    //json
    implementation("com.beust:klaxon:5.6")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}