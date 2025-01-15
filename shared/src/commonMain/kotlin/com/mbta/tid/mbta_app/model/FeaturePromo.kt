package com.mbta.tid.mbta_app.model

enum class FeaturePromo(val addedInVersion: AppVersion) {
    // ktfmt does not like empty enums as of the version we get with spotless
    Nothing(AppVersion(0u, 0u, 0u));

    companion object {
        fun featuresBetween(
            lastLaunchedVersion: AppVersion,
            currentVersion: AppVersion
        ): List<FeaturePromo> {
            return entries.filter {
                lastLaunchedVersion < it.addedInVersion && it.addedInVersion <= currentVersion
            }
        }
    }
}
