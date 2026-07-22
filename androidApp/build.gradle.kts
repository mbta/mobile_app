import com.diffplug.spotless.FormatterFunc
import com.mbta.tid.mbta_app.gradle.CheckMapboxBridgeTask
import com.mbta.tid.mbta_app.gradle.ConvertIosLocalizationTask
import com.mbta.tid.mbta_app.gradle.ConvertIosMapIconsTask
import com.mbta.tid.mbta_app.gradle.EnvReader
import java.io.Serializable
import java.net.URL
import java.util.Locale
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// To use debug signing keys and skip Sentry uploads for an easier time debugging
// performance-sensitive issues, turn this on.
val runLocalReleaseBuild = false

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose)
    alias(libs.plugins.mokkery)
    alias(libs.plugins.sentry.android)
    alias(libs.plugins.serialization)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
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
            val transitiveSentryCore = resolvedModuleVersions.find {
                it.module.toString() == "io.sentry:sentry"
            }
            checkNotNull(transitiveSentryCore) {
                    "Could not find io.sentry:sentry among ${resolvedModuleVersions.joinToString(prefix = "[", postfix = "]")}"
                }
                .version
        }
    }
}

android {
    namespace = "com.mbta.tid.mbta_app.android"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.mbta.tid.mbta_app"
        minSdk = 28
        targetSdk = 35
        versionCode =
            Integer.parseInt((findProperty("android.injected.version.code") ?: "1") as String)
        versionName = "2.1.1"
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
        create("local") {
            dimension = "environment"
            applicationIdSuffix = ".local"
        }

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
    implementation(libs.androidx.work)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.placeholder.material3)
    implementation(libs.compose.shimmer)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.installations)
    implementation(libs.firebase.messaging)
    implementation(libs.javaPhoenixClient)
    implementation(libs.koin.androidxCompose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.lottie.compose)
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

tasks.cyclonedxDirectBom {
    includeConfigs = listOf("stagingReleaseRuntimeClasspath")
    includeLicenseText = true
}

spotless {
    kotlin {
        custom(
            "use custom default wait timeout in android UI tests",
            object : Serializable, FormatterFunc.NeedsFile {
                val replacements =
                    mapOf(
                        "waitUntil(" to "waitUntilDefaultTimeout(",
                        "waitUntil {" to "waitUntilDefaultTimeout {",
                        "waitUntilAtLeastOneExists(" to "waitUntilAtLeastOneExistsDefaultTimeout(",
                        "waitUntilDoesNotExist(" to "waitUntilDoesNotExistDefaultTimeout(",
                        "waitUntilExactlyOneExists(" to "waitUntilExactlyOneExistsDefaultTimeout(",
                        "waitUntilNodeCount(" to "waitUntilNodeCountDefaultTimeout(",
                        ".assertIsDisplayed()" to ".assertCanBeDisplayed()",
                    )

                override fun applyWithFile(text: String, file: File): String {
                    if (!file.path.contains("androidTest") || file.path.contains("testUtils"))
                        return text
                    return replacements
                        .toList()
                        .fold(text, { text, (bad, good) -> text.replace(bad, good) })
                }
            },
        )
    }
}

tasks.register<ConvertIosMapIconsTask>("convertIosIconsToAssets") {
    assetsToRender = listOf("alert-large-*", "alert-small-*", "map-stop-*")
    assetsToReturnByName = listOf("alert-borderless-*")
}

tasks.register<ConvertIosLocalizationTask>("convertIosLocalization") {
    androidEnglishStrings = layout.projectDirectory.file("src/main/res/values/strings.xml")
    xcstrings = layout.projectDirectory.file("../iosApp/iosApp/Localizable.xcstrings")
    resources = layout.projectDirectory.dir("src/main/res")
}

tasks.register<CheckMapboxBridgeTask>("checkMapboxBridge") {
    mapboxBridgePath =
        layout.projectDirectory.file(
            "src/main/java/com/mbta/tid/mbta_app/android/map/MapboxBridge.kt"
        )
    sharedMapStylePackage =
        layout.projectDirectory.dir(
            "../shared/src/commonMain/kotlin/com/mbta/tid/mbta_app/map/style"
        )
}

// https://github.com/mapbox/mapbox-gl-native-android/blob/7f03a710afbd714368084e4b514d3880bad11c27/gradle/gradle-config.gradle
// set a temporary token so that the map can still load with cached tiles if for some reason
// dynamically fetching a real token fails.
tasks.register("mapboxTempToken") {
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

// we want to load environment variables while first declaring settings
run {
    val env = EnvReader()

    android.defaultConfig.buildConfigField(
        "String",
        "SENTRY_DSN",
        "\"${env["SENTRY_DSN_ANDROID"] ?: ""}\"",
    )

    // https://stackoverflow.com/a/53261807
    val sentryEnv =
        listOf("debug", "dev-orange", "staging", "prod").firstOrNull { env ->
            gradle.startParameter.taskNames.any {
                it.lowercase(Locale.getDefault()).contains(env.replace("-", ""))
            }
        } ?: "debug"

    val sentryEnvOverride: String = env["SENTRY_ENVIRONMENT"] ?: sentryEnv

    android.defaultConfig.buildConfigField(
        "String",
        "SENTRY_ENVIRONMENT",
        "\"${sentryEnvOverride}\"",
    )

    val firebaseKey = env["FIREBASE_KEY"]
    val googleAppId = env["GOOGLE_APP_ID_ANDROID"]
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

    val localBackendOrigin = env["LOCAL_BACKEND_ORIGIN_ANDROID"] ?: "http://10.0.2.2:4000"
    val localBackendOriginUrl = URL(localBackendOrigin)
    val networkSecurityConfigContent =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <network-security-config xmlns:android="http://schemas.android.com/apk/res/android">
            <domain-config cleartextTrafficPermitted="true">
                <domain includeSubdomains="true">${localBackendOriginUrl.host}</domain>
            </domain-config>
        </network-security-config>
    """
            .trimIndent()
    val networkSecurityConfigDir =
        layout.buildDirectory.dir("generated/networkSecurityConfig").get()
    networkSecurityConfigDir.dir("xml").asFile.mkdirs()
    networkSecurityConfigDir
        .file("xml/network_security_config.xml")
        .asFile
        .writeText(networkSecurityConfigContent)
    android.sourceSets.getByName("local") {
        res.directories.add(networkSecurityConfigDir.toString())
    }
}

gradle.projectsEvaluated {
    tasks
        .getByPath("preBuild")
        .dependsOn("mapboxTempToken", "convertIosIconsToAssets", "convertIosLocalization")
    tasks.getByPath("spotlessKotlin").mustRunAfter("convertIosLocalization")
    tasks.getByPath("check").dependsOn("checkMapboxBridge")
}
