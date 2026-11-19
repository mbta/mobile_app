package com.mbta.tid.mbta_app.routes

import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.RouteStopFilter
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

    @Serializable public data class StopOverview(val stopId: String) : SheetRoutes()

    @Serializable
    public data class RouteStop(
        val stopId: String,
        val routeStopFilter: RouteStopFilter,
        val tripFilter: TripDetailsFilter?,
    ) : SheetRoutes()

    @Serializable
    public data class RoutePicker(val path: RoutePickerPath, val context: RouteDetailsContext) :
        SheetRoutes()

    @Serializable
    public data class RouteDetails(val routeId: LineOrRoute.Id, val context: RouteDetailsContext) :
        SheetRoutes()

    @Serializable public data object EditFavorites : SheetRoutes()

    @Serializable public data class TripDetails(val filter: TripDetailsPageFilter) : SheetRoutes()

    public val showCurrentLocation: Boolean
        get() =
            when (this) {
                is TripDetails -> false
                else -> true
            }

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

    public val vehicleId: String?
        get() =
            when (this) {
                is RouteStop -> this.tripFilter?.vehicleId
                is TripDetails -> this.filter.vehicleId
                else -> null
            }

    public val hasFloatingBackButton: Boolean
        get() =
            when (this) {
                is StopOverview,
                is RouteStop,
                is TripDetails -> true
                is RouteDetails,
                is EditFavorites,
                is Favorites,
                is NearbyTransit,
                is RoutePicker -> false
            }

    public companion object {

        public fun shouldResetSheetHeight(first: SheetRoutes?, second: SheetRoutes?): Boolean {
            return !retainSheetSize(first, second) && pageChanged(first, second)
        }

        /**
         * Whether the page within the nearby transit tab changed. Moving from
         * StopOverview/RouteStop to StopOverview/RouteStop is only considered a page change if the
         * stopId changed.
         */
        internal fun pageChanged(first: SheetRoutes?, second: SheetRoutes?): Boolean {
            val firstStopId =
                when (first) {
                    is StopOverview -> first.stopId
                    is RouteStop -> first.stopId
                    else -> null
                }
            val secondStopId =
                when (second) {
                    is StopOverview -> second.stopId
                    is RouteStop -> second.stopId
                    else -> null
                }
            return if (firstStopId != null && secondStopId != null) {
                firstStopId != secondStopId
            } else {
                first != second
            }
        }

        /**
         * We want to retain sheet size unless moving into or out of stop overview/route stop, or
         * between tabs
         */
        internal fun retainSheetSize(first: SheetRoutes?, second: SheetRoutes?): Boolean {
            val transitionSet = setOf(first?.let { it::class }, second?.let { it::class })
            return !transitionSet.contains(StopOverview::class) &&
                !transitionSet.contains(RouteStop::class) &&
                transitionSet != setOf(NearbyTransit::class, Favorites::class) &&
                transitionSet != setOf(NearbyTransit::class) &&
                transitionSet != setOf(Favorites::class)
        }
    }
}
