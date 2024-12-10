import com.mbta.tid.mbta_app.gradle.ConvertIosMapIconsTask
import java.io.BufferedReader
import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose)
    alias(libs.plugins.cycloneDx)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.sentryGradle)
    alias(libs.plugins.serialization)
    id("check-mapbox-bridge")
}

sentry {
    // Generates a JVM (Java, Kotlin, etc.) source bundle and uploads your source code to Sentry.
    // This enables source context, allowing you to see your source
    // code as part of your stack traces in Sentry.
    includeSourceContext = true

    org = "mbtace"
    projectName = "mobile_app_android"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}

android {
    namespace = "com.mbta.tid.mbta_app.android"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.mbta.tid.mbta_app"
        minSdk = 28
        targetSdk = 34
        versionCode =
            Integer.parseInt((findProperty("android.injected.version.code") ?: "1") as String)
        versionName = (findProperty("android.injected.version.name") ?: "0.1.0") as String
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
    flavorDimensions += "environment"
    productFlavors {
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
        }

        create("prod") { dimension = "environment" }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
}

dependencies {
    implementation(projects.shared)
    implementation(platform(libs.compose.bom))
    implementation(platform(libs.koin.bom))
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.javaPhoenixClient)
    implementation(libs.koin.androidxCompose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.mapbox.android)
    implementation(libs.mapbox.compose)
    implementation(libs.mapbox.turf)
    implementation(libs.okhttp)
    implementation(libs.playServices.location)
    debugImplementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.koin.test)
    implementation(libs.koin.junit4)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.monitor)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.ktor.client.mock)
}

tasks.cyclonedxBom {
    includeConfigs =
        listOf("implementationDependenciesMetadata", "releaseImplementationDependenciesMetadata")
}

task<ConvertIosMapIconsTask>("convertIosIconsToAssets") {
    assetsToRender = listOf("alert-large-*", "alert-small-*", "map-stop-*")
    assetsToReturnByName = listOf("alert-borderless-*")
}

// https://github.com/mapbox/mapbox-gl-native-android/blob/7f03a710afbd714368084e4b514d3880bad11c27/gradle/gradle-config.gradle
task("accessToken") {
    val tokenFile = File("${projectDir}/src/main/res/values/secrets.xml")
    if (!tokenFile.exists()) {
        var mapboxAccessToken = System.getenv()["MAPBOX_PUBLIC_TOKEN"]
        if (mapboxAccessToken == null) {
            Logging.getLogger(this.javaClass)
                .warn("You should set the MAPBOX_PUBLIC_TOKEN environment variable.")
            mapboxAccessToken = ""
        }
        val tokenFileContents =
            """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="mapbox_access_token" translatable="false">$mapboxAccessToken</string>
</resources>"""
        tokenFile.writeText(tokenFileContents)
    }
}

task("envVars") {
    val envFile = File(".envrc")
    val props = Properties()

    if (envFile.exists()) {
        val bufferedReader: BufferedReader = envFile.bufferedReader()
        bufferedReader.use {
            it.readLines()
                .filter { line -> line.contains("export") }
                .map { line ->
                    val cleanLine = line.replace("export", "")
                    props.load(StringReader(cleanLine))
                }
        }
    } else {
        println(".envrc file not configured, reading from system env instead")
    }

    android.defaultConfig.buildConfigField(
        "String",
        "SENTRY_DSN",
        "\"${props.getProperty("SENTRY_DSN_ANDROID")
                ?: System.getenv("SENTRY_DSN_ANDROID") ?: ""}\""
    )

    android.defaultConfig.buildConfigField(
        "String",
        "SENTRY_ENVIRONMENT",
        "\"${props.getProperty("SENTRY_ENVIRONMENT")
                ?: System.getenv("SENTRY_ENVIRONMENT") ?: ""}\""
    )
}

gradle.projectsEvaluated {
    tasks.getByPath("preBuild").dependsOn("accessToken", "convertIosIconsToAssets")
    tasks.getByPath("check").dependsOn("checkMapboxBridge")
}
