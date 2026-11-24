package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import kotlinx.serialization.Serializable

public interface RouteDirection {
    public val routeId: LineOrRoute.Id
    public val directionId: Int
}

@Serializable
public data class RouteStopFilter
@DefaultArgumentInterop.Enabled
constructor(
    override val routeId: LineOrRoute.Id,
    override val directionId: Int,
    val autoFilter: Boolean = false,
) : RouteDirection {
    public companion object {
        /**
         * When the stop filter changes, we want a new entry to be added (i.e. no pop) only when
         * you're on the stop overview (lastFilter == nil) page, but if there is already a filter,
         * or the new entry is an auto filter, the entry with the old filter should be popped and
         * replaced with the new value.
         */
        public fun shouldPopLastStopEntry(
            lastFilter: RouteStopFilter?,
            newFilter: RouteStopFilter?,
        ): Boolean {
            return lastFilter != null || newFilter?.autoFilter == true
        }
    }
}

@Serializable
public data class TripDetailsFilter(
    val tripId: String,
    val vehicleId: String?,
    val stopSequence: Int?,
    // This is true when manually selecting a vehicle, so that route stop continues focusing on
    // a vehicle selected from the map, even after the prediction for that vehicle isn't displayed
    val selectionLock: Boolean = false,
)

public sealed class StopCentricPageFilters {
    public abstract val stopId: String
}

@Serializable
public data class StopOverviewPageFilters(override val stopId: String) : StopCentricPageFilters()

@Serializable
public data class RouteStopPageFilters(
    override val stopId: String,
    val routeStopFilter: RouteStopFilter,
    val tripFilter: TripDetailsFilter?,
) : StopCentricPageFilters() {
    public val routeStopDirection: RouteStopDirection =
        RouteStopDirection(routeStopFilter.routeId, stopId, routeStopFilter.directionId)
}

@Serializable
public data class TripDetailsPageFilter(
    val tripId: String,
    val vehicleId: String?,
    val routeId: LineOrRoute.Id,
    val directionId: Int,
    val stopId: String,
    val stopSequence: Int?,
) {
    val tripDetailsFilter: TripDetailsFilter
        get() = TripDetailsFilter(tripId, vehicleId = vehicleId, stopSequence = stopSequence)

    val routeStopFilter: RouteStopFilter
        get() = RouteStopFilter(routeId = routeId, directionId = directionId)

    public constructor(
        stopId: String,
        routeStopFilter: RouteStopFilter,
        tripFilter: TripDetailsFilter,
    ) : this(
        tripFilter.tripId,
        tripFilter.vehicleId,
        routeStopFilter.routeId,
        routeStopFilter.directionId,
        stopId,
        tripFilter.stopSequence,
    )
}
