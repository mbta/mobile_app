plugins {
    kotlin("native.cocoapods")
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.serialization)
    alias(libs.plugins.skie)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    targetHierarchy.default()

    androidTarget { compilations.all { kotlinOptions { jvmTarget = "1.8" } } }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Common library for the MBTA mobile app"
        homepage = "https://github.com/mbta/mobile_app"
        version = "1.0"
        ios.deploymentTarget = "15.0"
        podfile = project.file("../iosApp/Podfile")
        pod("Sentry", "~> 8.17.2")

        framework {
            baseName = "shared"
            binaryOption("bundleId", "com.mbta.tid.mobileapp")
            export(libs.kotlinx.datetime)
        }
    }

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
                implementation(libs.sentry)
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
}

android {
    namespace = "com.mbta.tid.mbta_app"
    compileSdk = 34
    defaultConfig { minSdk = 28 }
}
