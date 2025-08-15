package com.mbta.tid.mbta_app.model

public enum class FeaturePromo(
    internal val addedInAndroidVersion: AppVersion,
    internal val addedInIosVersion: AppVersion,
) {

    CombinedStopAndTrip(AppVersion(0u, 0u, 0u), AppVersion(1u, 2u, 0u)),
    EnhancedFavorites(AppVersion(2u, 0u, 0u), AppVersion(2u, 0u, 0u));

    constructor(addedInVersion: AppVersion) : this(addedInVersion, addedInVersion)

    internal companion object {
        fun featuresBetween(
            lastLaunchedVersion: AppVersion,
            currentVersion: AppVersion,
        ): List<FeaturePromo> {
            return entries.filter {
                lastLaunchedVersion < it.addedInVersion && it.addedInVersion <= currentVersion
            }
        }
    }
}

internal expect val FeaturePromo.addedInVersion: AppVersion
