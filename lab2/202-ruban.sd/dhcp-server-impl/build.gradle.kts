plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "net.fennmata"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}
