import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    // trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidApplication).apply(false)
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.cycloneDx)
    alias(libs.plugins.kotlinAndroid).apply(false)
    alias(libs.plugins.kotlinCocoapods).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.spotless)
}

spotless { kotlinGradle { ktlint() } }

subprojects {
    apply(plugin = "com.diffplug.spotless")

    spotless {
        kotlin {
            target("src/**/*.kt")
            ktfmt().kotlinlangStyle()
        }
        kotlinGradle { ktfmt().kotlinlangStyle() }
    }

    tasks.withType<AbstractTestTask> {
        testLogging {
            // set options for log level LIFECYCLE
            events =
                setOf(
                    TestLogEvent.FAILED,
                    TestLogEvent.PASSED,
                    TestLogEvent.SKIPPED,
                    TestLogEvent.STANDARD_OUT,
                )

            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = true
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }
}

tasks.getByName("clean", Delete::class) { delete(rootProject.layout.buildDirectory) }
