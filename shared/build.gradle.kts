plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.serialization)
    alias(libs.plugins.skie)
    kotlin("native.cocoapods")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    targetHierarchy.default()

    androidTarget { compilations.all { kotlinOptions { jvmTarget = "1.8" } } }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.encoding)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.websockets)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.datetime)
                api(libs.sentry)
                implementation(libs.skie.configuration.annotations)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.ktor.client.mock)
            }
        }
        val androidMain by getting { dependencies { implementation(libs.ktor.client.okhttp) } }
        val iosMain by getting { dependencies { implementation(libs.ktor.client.darwin) } }
    }

    cocoapods {
        // Required properties
        // Specify the required Pod version here. Otherwise, the Gradle project version is used.
        version = "1.0"
        summary = "Some description for a Kotlin/Native module"
        homepage = "Link to a Kotlin/Native module homepage"
        ios.deploymentTarget = "14.1"

        framework {
            // Required properties
            baseName = "shared"
            binaryOption("bundleId", "com.mbta.tid.mobileapp")
            // Dependency export
            export(libs.kotlinx.datetime)
            export(libs.sentry)
            export(project(":shared"))


        }

        podfile = project.file("../iosApp/Podfile")
        // Make sure you use the proper version according to our Cocoa SDK Version Compatibility Table.
        pod("Sentry") {
            version = "~> 8.17.2"

        }

        // Maps custom Xcode configuration to NativeBuildType
   //          xcodeConfigurationToNativeBuildType["CUSTOM_DEBUG"] = NativeBuildType.DEBUG
    //         xcodeConfigurationToNativeBuildType["CUSTOM_RELEASE"] = NativeBuildType.RELEASE
    }
}

android {
    namespace = "com.mbta.tid.mbta_app"
    compileSdk = 34
    defaultConfig { minSdk = 28 }
}
