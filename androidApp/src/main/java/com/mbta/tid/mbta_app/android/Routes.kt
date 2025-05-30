package com.mbta.tid.mbta_app.android

import androidx.navigation.NavBackStackEntry
import kotlinx.serialization.Serializable

sealed class Routes {
    @Serializable data object MapAndSheet : Routes()

    @Serializable data object More : Routes()

    companion object {
        fun fromNavBackStackEntry(backStackEntry: NavBackStackEntry?): Routes {
            val route = backStackEntry?.destination?.route
            return if (route?.contains("More") == true) {
                Routes.More
            } else {
                Routes.MapAndSheet
            }
        }
    }
}
