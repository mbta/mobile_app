plugins {
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.serialization)
    alias(libs.plugins.skie)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget { compilations.all { kotlinOptions { jvmTarget = "1.8" } } }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Shared test utilities for the MBTA mobile app"
        homepage = "https://github.com/mbta/mobile_app"
        version = "1.0"
        ios.deploymentTarget = "15.0"
        podfile = project.file("../iosApp/Podfile")
        pod("shared", path = projects.shared.dependencyProject.projectDir)

        framework {
            baseName = "sharedTestUtil"
            binaryOption("bundleId", "com.mbta.tid.mobileapp.testutil")
        }
    }

    tasks
        .getByName("generateDefShared")
        .dependsOn(
            ":shared:commonizeCInterop",
            ":shared:cinteropSentryIosArm64",
            ":shared:cinteropSentryIosSimulatorArm64",
            ":shared:cinteropSentryIosX64",
            ":shared:podSetupBuildSentryIphoneos",
            ":shared:compileKotlinIosSimulatorArm64",
        )
    tasks
        .getByName("podspec")
        .dependsOn(
            ":shared:commonizeCInterop",
            ":shared:cinteropSentryIosArm64",
            ":shared:cinteropSentryIosSimulatorArm64",
            ":shared:cinteropSentryIosX64",
            ":shared:podSetupBuildSentryIphoneos",
            ":shared:generateDummyFramework",
            ":shared:compileKotlinIosSimulatorArm64",
        )
    tasks.getByName("podInstallSyntheticIos").dependsOn(":shared:generateDummyFramework")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(libs.skie.configuration.annotations)
            }
        }
        val commonTest by getting { dependencies {} }
        val androidMain by getting { dependencies {} }
        val iosMain by getting { dependencies {} }
    }
}

android {
    namespace = "com.mbta.tid.mbta_app"
    compileSdk = 34
    defaultConfig { minSdk = 28 }
}
