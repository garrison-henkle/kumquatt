@file:Suppress("UnstableApiUsage")

import com.vanniktech.maven.publish.SonatypeHost.S01
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "tech.ghenkle"
version = "1.0.0"

buildscript{
    repositories{
        mavenCentral()
    }

    dependencies{
        classpath("com.vanniktech:gradle-maven-publish-plugin")
    }
}

plugins {
    kotlin("jvm") version "1.6.21" apply false
    signing
    id("com.vanniktech.maven.publish.base") version "0.19.0"
}

allprojects {
    apply{ plugin("com.vanniktech.maven.publish.base") }

    plugins.withId("com.vanniktech.maven.publish.base") {
        group = "tech.ghenkle"
        version = "1.0.0"

        mavenPublishing {
            publishToMavenCentral(S01)
            signAllPublications()

            pom{
                name.set("Kumquatt")
                description.set("A Kotlin wrapper for the Eclipse Paho MQTT client library.")
                inceptionYear.set("2022")
                url.set("https://github.com/garrison-henkle/kumquatt/")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("garrison-henkle")
                        name.set("Garrison Henkle")
                        url.set("https://github.com/garrison-henkle/")
                    }
                }
                scm{
                    url.set("https://github.com/garrison-henkle/kumquatt/")
                    connection.set("scm:git:git://github.com/garrison-henkle/kumquatt.git")
                    developerConnection.set("scm:git:ssh://git@github.com/garrison-henkle/kumquatt.git")
                }
            }
        }
    }
}

subprojects{
    apply{
        plugin("org.jetbrains.kotlin.jvm")
        plugin("maven-publish")
    }

    repositories {
        mavenCentral()
    }

    val implementation by configurations
    dependencies {
        implementation(kotlin("stdlib-jdk8"))
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
}
