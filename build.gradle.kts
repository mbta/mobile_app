import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    // trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidApplication).apply(false)
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.cycloneDx)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.spotless)
    alias(libs.plugins.testLogger)
}

spotless { kotlinGradle { ktlint() } }

subprojects {
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "com.adarshr.test-logger")

    spotless {
        kotlin {
            target("src/**/*.kt")
            ktfmt().kotlinlangStyle()
        }
        kotlinGradle { ktfmt().kotlinlangStyle() }
    }
    testlogger {
        showPassed = false
        showSkipped = false
    }
}

tasks.getByName("clean", Delete::class) { delete(rootProject.layout.buildDirectory) }
