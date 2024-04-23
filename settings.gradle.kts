enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://androidx.dev/storage/compose-compiler/repository/")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://androidx.dev/storage/compose-compiler/repository/")

        val mapboxToken =
            providers.gradleProperty("MAPBOX_SECRET_TOKEN").orNull
                ?: System.getenv("MAPBOX_SECRET_TOKEN")
        if (mapboxToken != null) {
            maven {
                url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
                credentials.username = "mapbox"
                credentials.password = mapboxToken
                authentication.create<BasicAuthentication>("basic")
            }
        } else {
            Logging.getLogger(Settings::class.java)
                .warn(
                    "MAPBOX_SECRET_TOKEN not found, see https://docs.mapbox.com/android/maps/guides/install/#configure-credentials",
                )
        }
    }
}

rootProject.name = "MBTA_App"

include(":androidApp")

include(":shared")
