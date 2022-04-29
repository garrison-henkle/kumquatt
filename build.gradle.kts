import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21" apply false
}

subprojects{
    group = "tech.ghenkle.kumquatt"
    version = "1.0.0"

    apply{
        plugin("org.jetbrains.kotlin.jvm")
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
