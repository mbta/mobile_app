import co.touchlab.skie.configuration.DefaultArgumentInterop
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.mokkery)
    alias(libs.plugins.sentry)
    alias(libs.plugins.serialization)
    alias(libs.plugins.skie)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_1_8) } }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Common library for the MBTA mobile app"
        homepage = "https://github.com/mbta/mobile_app"
        version = "1.0"
        ios.deploymentTarget = "15.0"
        podfile = project.file("../iosApp/Podfile")

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
                api(libs.kotlinx.datetime)
                api(libs.sentry)
                api(libs.spatialk.geojson)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.androidx.datastore.preferences.core)
                implementation(libs.koin.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.encoding)
                implementation(libs.ktor.client.websockets)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.okio)
                implementation(libs.skie.configuration.annotations)
                implementation(libs.spatialk.turf)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.koin.test)
                implementation(libs.kotlin.test)
                implementation(libs.ktor.client.mock)
                implementation(libs.okio.fakefilesystem)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.koin.androidxCompose)
                implementation(libs.ktor.client.okhttp)
            }
        }
        val iosMain by getting { dependencies { implementation(libs.ktor.client.darwin) } }
    }
}

android {
    namespace = "com.mbta.tid.mbta_app"
    compileSdk = 34
    defaultConfig { minSdk = 28 }
}

skie { features { group { DefaultArgumentInterop.MaximumDefaultArgumentCount(8) } } }

mokkery {
    ignoreInlineMembers.set(true)
    ignoreFinalMembers.set(true)
}

sentryKmp { autoInstall.commonMain.enabled = false }
