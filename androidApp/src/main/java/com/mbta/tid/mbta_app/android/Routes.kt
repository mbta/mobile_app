package com.mbta.tid.mbta_app.android

import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import com.mbta.tid.mbta_app.model.morePage.MoreSection
import kotlinx.serialization.Serializable

sealed class Routes {
    @Serializable data object MapAndSheet : Routes()

    @Serializable data class More(val highlight: MoreSection.Category? = null) : Routes()

    companion object {
        fun fromNavBackStackEntry(backStackEntry: NavBackStackEntry?): Routes {
            val route = backStackEntry?.destination?.route
            return if (route?.contains("More") == true) {
                backStackEntry.toRoute<More>()
            } else {
                MapAndSheet
            }
        }
    }
}
