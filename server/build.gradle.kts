plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
}

group = "io.github.loeswerkman.language_trainer"
version = "1.0.0"
application {
    mainClass = "io.github.loeswerkman.language_trainer.ApplicationKt"
}

// Run from the repository root so the default `vocabulary/` and `data/` paths resolve correctly.
tasks.named<JavaExec>("run") {
    workingDir = rootDir
}

sourceSets.test {
    resources.srcDir(rootProject.file("vocabulary"))
}

dependencies {
    api(project(":core"))
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serializationKotlinxJson)
    implementation(libs.kaml)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.sqlite.jdbc)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}
