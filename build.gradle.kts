plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.21"
}

group = "archives.tater.bot.bridge"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${properties["serialization_version"]}")
    implementation("dev.kord:kord-core:${properties["kord_version"]}")
    implementation("io.github.cdimascio:dotenv-kotlin:${properties["dotenv_version"]}")
    implementation("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}