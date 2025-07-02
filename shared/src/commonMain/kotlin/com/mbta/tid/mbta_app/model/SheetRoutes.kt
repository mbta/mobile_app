package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import kotlinx.serialization.Serializable

@Serializable
sealed class SheetRoutes {

    @Serializable sealed interface Entrypoint

    @Serializable data object Favorites : SheetRoutes(), Entrypoint

    @Serializable data object NearbyTransit : SheetRoutes(), Entrypoint

    @Serializable
    data class StopDetails(
        val stopId: String,
        val stopFilter: StopDetailsFilter?,
        val tripFilter: TripDetailsFilter?,
    ) : SheetRoutes()

    @Serializable
    data class RoutePicker(val path: RoutePickerPath, val context: RouteDetailsContext) :
        SheetRoutes()

    @Serializable
    data class RouteDetails(val routeId: String, val context: RouteDetailsContext) : SheetRoutes()

    @Serializable data object EditFavorites : SheetRoutes()

    val showSearchBar: Boolean
        get() =
            when (this) {
                is Favorites -> true
                is NearbyTransit -> true
                else -> false
            }

    companion object {
        /**
         * Whether the page within the nearby transit tab changed. Moving from StopDetails to
         * StopDetails is only considered a page change if the stopId changed.
         */
        fun pageChanged(first: SheetRoutes?, second: SheetRoutes?): Boolean {
            return if (first is StopDetails && second is StopDetails) {
                first.stopId != second.stopId
            } else {
                first != second
            }
        }

        /** When transitioning between certain routes, we don't want to resize the sheet */
        fun retainSheetSize(first: SheetRoutes?, second: SheetRoutes?): Boolean {
            val transitionSet = setOf(first?.let { it::class }, second?.let { it::class })
            return transitionSet == setOf(RoutePicker::class) ||
                transitionSet == setOf(RouteDetails::class, RoutePicker::class)
        }
    }
}
