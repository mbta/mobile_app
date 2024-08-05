plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "1.9.20"
    id("com.diffplug.spotless").version("6.21.0")
}

dependencies { implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1") }

spotless {
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint()
    }
}
