@file:Suppress("UnstableApiUsage")

import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import org.jetbrains.dokka.gradle.DokkaTask

plugins{
    `java-library`
    id("org.jetbrains.dokka") version "1.6.21"
}

dependencies {
    //kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")

    //networking
    api("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    //json
    implementation("com.beust:klaxon:5.6")
}

/* Sources */

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

/* Javadoc */

val dokkaOutputDir = "$buildDir/dokka"
val dokka = tasks.getByName<DokkaTask>("dokkaHtml") {
    outputDirectory.set(file(dokkaOutputDir))
}

val deleteDokkaOutputDir by tasks.register<Delete>("deleteDokkaOutputDirectory") {
    delete(dokkaOutputDir)
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    dependsOn(deleteDokkaOutputDir, dokka)
    archiveClassifier.set("javadoc")
    from(dokkaOutputDir)
}

/* Assembling */

//base{
//    archivesName.set("kumquatt")
//}

artifacts {
    archives(sourcesJar)
    archives(javadocJar)
    archives(tasks.jar)
}

/* Publishing */

publishing{
    publications.withType<MavenPublication>{
        artifactId = "kumquatt"
    }
}

mavenPublishing{
    configure(
        KotlinJvm(
            javadocJar = Dokka("dokkaHtml"),
            sourcesJar = true
        )
    )
}
