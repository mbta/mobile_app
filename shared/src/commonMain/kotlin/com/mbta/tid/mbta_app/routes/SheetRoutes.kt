package com.mbta.tid.mbta_app.routes

import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import kotlinx.serialization.Serializable

@Serializable
public sealed class SheetRoutes {

    @Serializable public sealed interface Entrypoint

    @Serializable public data object Favorites : SheetRoutes(), Entrypoint

    @Serializable public data object NearbyTransit : SheetRoutes(), Entrypoint

    @Serializable
    public data class StopDetails(
        val stopId: String,
        val stopFilter: StopDetailsFilter?,
        val tripFilter: TripDetailsFilter?,
    ) : SheetRoutes()

    @Serializable
    public data class RoutePicker(val path: RoutePickerPath, val context: RouteDetailsContext) :
        SheetRoutes()

    @Serializable
    public data class RouteDetails(val routeId: String, val context: RouteDetailsContext) :
        SheetRoutes()

    @Serializable public data object EditFavorites : SheetRoutes()

    @Serializable public data class TripDetails(val filter: TripDetailsPageFilter) : SheetRoutes()

    public val showSearchBar: Boolean
        get() =
            when (this) {
                is Favorites,
                is NearbyTransit -> true
                else -> false
            }

    public val allowTargeting: Boolean
        get() =
            when (this) {
                is Favorites,
                is NearbyTransit -> true
                else -> false
            }

    public companion object {

        public fun shouldResetSheetHeight(first: SheetRoutes?, second: SheetRoutes?): Boolean {
            return !retainSheetSize(first, second) && pageChanged(first, second)
        }

        /**
         * Whether the page within the nearby transit tab changed. Moving from StopDetails to
         * StopDetails is only considered a page change if the stopId changed.
         */
        public fun pageChanged(first: SheetRoutes?, second: SheetRoutes?): Boolean {
            return if (first is StopDetails && second is StopDetails) {
                first.stopId != second.stopId
            } else {
                first != second
            }
        }

        /**
         * We want to retain sheet size unless moving into or out of stop details, or between tabs
         */
        internal fun retainSheetSize(first: SheetRoutes?, second: SheetRoutes?): Boolean {
            val transitionSet = setOf(first?.let { it::class }, second?.let { it::class })
            return !transitionSet.contains(StopDetails::class) &&
                transitionSet != setOf(NearbyTransit::class, Favorites::class) &&
                transitionSet != setOf(NearbyTransit::class) &&
                transitionSet != setOf(Favorites::class)
        }
    }
}
