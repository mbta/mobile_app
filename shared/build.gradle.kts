import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.kotlinCocoapods)
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
        pod("Sentry", "~> 8.20.0")

        framework {
            baseName = "shared"
            binaryOption("bundleId", "com.mbta.tid.mobileapp")
            export(libs.kotlinx.datetime)
            export(libs.sentry)
        }

        xcodeConfigurationToNativeBuildType["StagingDebug"] = NativeBuildType.DEBUG
        xcodeConfigurationToNativeBuildType["StagingRelease"] = NativeBuildType.RELEASE
        xcodeConfigurationToNativeBuildType["ProdDebug"] = NativeBuildType.DEBUG
        xcodeConfigurationToNativeBuildType["ProdRelease"] = NativeBuildType.RELEASE
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.encoding)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.websockets)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.datetime)
                api(libs.sentry)
                implementation(libs.skie.configuration.annotations)
                api(libs.spatialk.geojson)
                implementation(libs.spatialk.turf)
                implementation(libs.androidx.datastore.preferences.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.koin.test)
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
