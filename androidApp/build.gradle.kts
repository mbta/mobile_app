import com.diffplug.spotless.FormatterFunc
import com.mbta.tid.mbta_app.gradle.ConvertIosLocalizationTask
import com.mbta.tid.mbta_app.gradle.ConvertIosMapIconsTask
import java.io.BufferedReader
import java.io.Serializable
import java.io.StringReader
import java.util.Locale
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// To use debug signing keys and skip Sentry uploads for an easier time debugging
// performance-sensitive issues, turn this on.
val runLocalReleaseBuild = false

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose)
    alias(libs.plugins.cycloneDx)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.mokkery)
    alias(libs.plugins.sentry.android)
    alias(libs.plugins.serialization)
    id("check-mapbox-bridge")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

sentry {
    // Generates a JVM (Java, Kotlin, etc.) source bundle and uploads your source code to Sentry.
    // This enables source context, allowing you to see your source
    // code as part of your stack traces in Sentry.
    includeSourceContext = !runLocalReleaseBuild

    org = "mbtace"
    projectName = "mobile_app_android"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
    autoInstallation {
        sentryVersion = provider {
            val bareKMPConfig = configurations.detachedConfiguration(libs.sentry.kmp.get())
            val resolvedDependencies = bareKMPConfig.incoming.resolutionResult
            val resolvedModuleVersions =
                resolvedDependencies.allComponents.mapNotNull { it.moduleVersion }
            val transitiveSentryCore =
                resolvedModuleVersions.find { it.module.toString() == "io.sentry:sentry" }
            checkNotNull(transitiveSentryCore) {
                    "Could not find io.sentry:sentry among ${resolvedModuleVersions.joinToString(prefix = "[", postfix = "]")}"
                }
                .version
        }
    }
}

android {
    namespace = "com.mbta.tid.mbta_app.android"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.mbta.tid.mbta_app"
        minSdk = 28
        targetSdk = 35
        versionCode =
            Integer.parseInt((findProperty("android.injected.version.code") ?: "1") as String)
        versionName = "2.0.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (runLocalReleaseBuild) {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }
    flavorDimensions += "environment"
    productFlavors {
        create("devOrange") {
            dimension = "environment"
            applicationIdSuffix = ".devOrange"
        }

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
    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
}

dependencies {
    implementation(projects.shared)
    implementation(platform(libs.compose.bom))
    implementation(platform(libs.firebase.bom))
    implementation(platform(libs.koin.bom))
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.placeholder.material3)
    implementation(libs.compose.shimmer)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.firebase.analytics)
    implementation(libs.javaPhoenixClient)
    implementation(libs.koin.androidxCompose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.mapbox.android)
    implementation(libs.mapbox.compose)
    implementation(libs.mapbox.turf)
    implementation(libs.okhttp)
    implementation(libs.playServices.location)
    implementation(libs.androidx.lifecycle.runtime.testing)
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

spotless {
    kotlin {
        custom(
            "use custom default wait timeout in android UI tests",
            object : Serializable, FormatterFunc.NeedsFile {
                override fun applyWithFile(text: String, file: File): String {
                    if (
                        !file.path.contains("androidTest") ||
                            file.name == "ComposeTestRuleExtension.kt" ||
                            file.name == "RetryableComposeTestRule.kt"
                    )
                        return text
                    val lines = text.lines()
                    for (line in lines.withIndex()) {
                        for (fnText in
                            arrayOf(
                                "waitUntil(",
                                "waitUntil {",
                                "waitUntilAtLeastOneExists(",
                                "waitUntilDoesNotExist(",
                                "waitUntilExactlyOneExists(",
                                "waitUntilNodeCount(",
                            )) {
                            val column = line.value.indexOf(fnText, 0, false)
                            if (column != -1) {
                                throw IllegalStateException(
                                    "${file.path}:${line.index + 1}:${column + 1} calls $fnText. Replace with ${fnText.dropLast(1)}DefaultTimeout to prevent CI flakiness."
                                )
                            }
                        }
                    }
                    return text
                }
            },
        )
    }
}

task<ConvertIosMapIconsTask>("convertIosIconsToAssets") {
    assetsToRender = listOf("alert-large-*", "alert-small-*", "map-stop-*")
    assetsToReturnByName = listOf("alert-borderless-*")
}

task<ConvertIosLocalizationTask>("convertIosLocalization") {
    androidEnglishStrings = layout.projectDirectory.file("src/main/res/values/strings.xml")
    xcstrings = layout.projectDirectory.file("../iosApp/iosApp/Localizable.xcstrings")
    resources = layout.projectDirectory.dir("src/main/res")
}

// https://github.com/mapbox/mapbox-gl-native-android/blob/7f03a710afbd714368084e4b514d3880bad11c27/gradle/gradle-config.gradle
// set a temporary token so that the map can still load with cached tiles if for some reason
// dynamically fetching a real token fails.
task("mapboxTempToken") {
    val tokenFile = File("${projectDir}/src/main/res/values/secrets.xml")
    if (!tokenFile.exists()) {
        val tokenFileContents =
            """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="mapbox_access_token" translatable="false">"temporary_mapbox_token"</string>
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
                .filter { line -> line.startsWith("export") }
                .map { line ->
                    val cleanLine = line.replace("export", "")
                    props.load(StringReader(cleanLine))
                }
        }
    } else {
        println(".envrc file not configured, reading from system env instead")
    }

    fun getPropsOrEnv(key: String): String? = props.getProperty(key) ?: System.getenv(key)

    android.defaultConfig.buildConfigField(
        "String",
        "SENTRY_DSN",
        "\"${getPropsOrEnv("SENTRY_DSN_ANDROID") ?: ""}\"",
    )

    // https://stackoverflow.com/a/53261807
    val sentryEnv =
        listOf("debug", "dev-orange", "staging", "prod").firstOrNull { env ->
            gradle.startParameter.taskNames.any {
                it.lowercase(Locale.getDefault()).contains(env.replace("-", ""))
            }
        } ?: "debug"

    val sentryEnvOverride: String = getPropsOrEnv("SENTRY_ENVIRONMENT") ?: sentryEnv

    android.defaultConfig.buildConfigField(
        "String",
        "SENTRY_ENVIRONMENT",
        "\"${sentryEnvOverride}\"",
    )

    val firebaseKey = getPropsOrEnv("FIREBASE_KEY")
    val googleAppId = getPropsOrEnv("GOOGLE_APP_ID_ANDROID")
    if (firebaseKey != null && googleAppId != null) {
        val googleSecretsFile = File("${projectDir}/src/main/res/values/secrets-google.xml")
        val lines =
            listOfNotNull(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
                "<resources>",
                "    <string name=\"google_app_id\" translatable=\"false\">$googleAppId</string>",
                "    <string name=\"google_api_key\" translatable=\"false\">$firebaseKey</string>",
                "    <string name=\"google_crash_reporting_api_key\" translatable=\"false\">$firebaseKey</string>",
                "</resources>",
            )
        googleSecretsFile.writeText(lines.joinToString(separator = "\n"))
    } else {
        logger.warn("FIREBASE_KEY or GOOGLE_APP_ID_ANDROID not provided, skipping Firebase setup")
    }
}

gradle.projectsEvaluated {
    tasks
        .getByPath("preBuild")
        .dependsOn("mapboxTempToken", "convertIosIconsToAssets", "convertIosLocalization")
    tasks.getByPath("spotlessKotlin").mustRunAfter("convertIosLocalization")
    tasks.getByPath("check").dependsOn("checkMapboxBridge")
}
