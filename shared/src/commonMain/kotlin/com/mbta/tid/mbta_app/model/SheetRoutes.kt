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
                is Favorites,
                is NearbyTransit -> true
                else -> false
            }

    val allowTargeting: Boolean
        get() =
            when (this) {
                is Favorites,
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

        /**
         * We want to retain sheet size unless moving into or out of stop details, or between tabs
         */
        fun retainSheetSize(first: SheetRoutes?, second: SheetRoutes?): Boolean {
            val transitionSet = setOf(first?.let { it::class }, second?.let { it::class })
            return !transitionSet.contains(StopDetails::class) &&
                transitionSet != setOf(NearbyTransit::class, Favorites::class) &&
                transitionSet != setOf(NearbyTransit::class) &&
                transitionSet != setOf(Favorites::class)
        }
    }
}
