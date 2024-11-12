plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "1.9.22"
    id("com.diffplug.spotless").version("6.21.0")
}

dependencies {
    implementation("org.cyclonedx:cyclonedx-core-java:9.0.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

spotless {
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint()
    }
}
