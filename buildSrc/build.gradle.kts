plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "2.3.0"
    alias(libs.plugins.spotless)
}

dependencies {
    implementation(libs.cyclonedx.core.java)
    implementation(libs.kotlinx.serialization.json.compatibleWithGradleEmbeddedKotlin)
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
