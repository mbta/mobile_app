plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.ksp)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    targetHierarchy.default()

    androidTarget { compilations.all { kotlinOptions { jvmTarget = "1.8" } } }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "shared"
            binaryOption("bundleId", "com.mbta.tid.mobileapp")
        }
    }

    sourceSets {
        val commonMain by getting {
            resources.srcDirs("res")
            dependencies {
                // put your multiplatform dependencies here
                implementation(libs.lyricist)

            }
        }
        val commonTest by getting { dependencies { implementation(libs.kotlin.test) } }

    }
}

ksp {
    arg("lyricist.xml.resourcesPath", kotlin.sourceSets.commonMain.get().resources.srcDirs.first().absolutePath)
//    arg("lyricist.xml.resourcesPath", "res")
//    arg("lyricist.xml.resourcesPath", "src/commonMain/res")
    arg("lyricist.xml.moduleName", "xml")
    arg("lyricist.xml.defaultLanguageTag", "en")
    arg("lyricist.packageName", "com.mbta.tid.mbta_app")
    arg("lyricist.internalVisibility", "false")
    arg("lyricist.generateStringsProperty", "true")
}

dependencies {
//    add("kspCommonMainMetadata", libs.lyricist.processor.xml)
//    add("kspCommonMainMetadata", libs.lyricist.processor)

}

//// workaround for KSP only in Common Main.
//// https://github.com/google/ksp/issues/567
//tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().all {
//    if (name != "kspCommonMainKotlinMetadata") {
//        dependsOn("kspCommonMainKotlinMetadata")
//    }
//}

kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}

task("testClasses").doLast {
    println("This is a dummy testClasses task")
}

android {
    namespace = "com.mbta.tid.mbta_app"
    compileSdk = 34
    defaultConfig { minSdk = 28 }
}
