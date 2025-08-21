plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "2.0.20"
    id("com.diffplug.spotless").version("7.2.1")
}

dependencies {
    implementation("org.cyclonedx:cyclonedx-core-java:10.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

spotless {
    kotlin {
        target("**/*.kt")
        ktfmt().kotlinlangStyle()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktfmt().kotlinlangStyle()
    }
}
