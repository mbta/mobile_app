package com.mbta.tid.mbta_app.model

enum class FeaturePromo(val addedInAndroidVersion: AppVersion, val addedInIosVersion: AppVersion) {
    // ktfmt does not like empty enums as of the version we get with spotless
    Nothing(AppVersion(0u, 0u, 0u));

    constructor(addedInVersion: AppVersion) : this(addedInVersion, addedInVersion)

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

expect val FeaturePromo.addedInVersion: AppVersion
