plugins {
    kotlin("jvm") version "2.1.0"
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "com.nullswan"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation(kotlin("stdlib"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    jvmToolchain(21)
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("kotlin", "com.nullswan.cloudstorage.kotlin")
    relocate("org.jetbrains", "com.nullswan.cloudstorage.jetbrains")
    relocate("org.intellij", "com.nullswan.cloudstorage.intellij")
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
