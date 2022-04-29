plugins {
    application
}

dependencies{
    implementation(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
}

application {
    mainClass.set("tech.ghenkle.kumquatt.example.MainKt")
}