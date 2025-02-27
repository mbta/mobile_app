package com.mbta.tid.mbta_app.android

import androidx.navigation.NavBackStackEntry
import kotlinx.serialization.Serializable

sealed class Routes {
    @Serializable object NearbyTransit : Routes()

    @Serializable object More : Routes()

    companion object {
        fun fromNavBackStackEntry(backStackEntry: NavBackStackEntry?): Routes {

            return if (
                backStackEntry != null && backStackEntry.destination.route?.contains("More") == true
            ) {
                Routes.More
            } else {
                Routes.NearbyTransit
            }
        }
    }
}
