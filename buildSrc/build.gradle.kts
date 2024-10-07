plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "1.9.22"
    id("com.diffplug.spotless").version("6.21.0")
}

dependencies { implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3") }

spotless {
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint()
    }
}
