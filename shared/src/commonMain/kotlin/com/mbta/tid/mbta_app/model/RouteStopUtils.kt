package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant

public object RouteStopUtils {
    public fun autoTripFilter(
        routeCardData: List<RouteCardData>?,
        routeStopFilter: RouteStopFilter?,
        currentTripFilter: TripDetailsFilter?,
        filterAtTime: EasternTimeInstant,
        globalData: GlobalResponse?,
    ): TripDetailsFilter? {
        val route =
            routeCardData?.find { it.lineOrRoute.id == routeStopFilter?.routeId }
                ?: return currentTripFilter
        val leaf =
            route.stopData.singleOrNull()?.data?.find {
                it.direction.id == routeStopFilter?.directionId
            } ?: return currentTripFilter
        if (currentTripFilter?.selectionLock == true) return currentTripFilter

        val leafFormat = leaf.format(filterAtTime, globalData)
        val formats =
            when (leafFormat) {
                is LeafFormat.Single -> listOf(leafFormat.format)
                is LeafFormat.Branched -> leafFormat.branchRows.map { it.format }
            }
        val relevantTrips = formats.flatMap { format ->
            when (format) {
                is UpcomingFormat.Some -> format.trips.map { it.trip }
                else -> emptyList()
            }
        }

        val alreadySelectedTrip = relevantTrips.find { it.trip.id == currentTripFilter?.tripId }

        if (currentTripFilter != null && alreadySelectedTrip != null) {
            return currentTripFilter.copy(vehicleId = alreadySelectedTrip.vehicle?.id)
        }

        var filterTrip: UpcomingTrip = relevantTrips.firstOrNull() ?: return null
        var cancelIndex = 1
        while (filterTrip.isCancelled && relevantTrips.size > cancelIndex) {
            // If the auto trip filter would select a cancelled trip,
            // select the next uncancelled trip instead
            val nextTrip = relevantTrips[cancelIndex]
            if (!nextTrip.isCancelled) {
                filterTrip = nextTrip
            }
            cancelIndex++
        }
        return TripDetailsFilter(
            tripId = filterTrip.trip.id,
            vehicleId = filterTrip.vehicle?.id,
            stopSequence = filterTrip.stopSequence,
        )
    }

    public class ScreenReaderContext(
        public val routeType: RouteType,
        public val destination: String?,
        public val stopName: String,
    )

    public fun getScreenReaderTripDepartureContext(
        routeCardData: List<RouteCardData>?,
        previousFilters: RouteStopPageFilters,
    ): ScreenReaderContext? {
        val stopFilter = previousFilters.routeStopFilter
        val selectedRoute =
            routeCardData?.firstOrNull { it.lineOrRoute.id == stopFilter.routeId } ?: return null
        val selectedStop = selectedRoute.stopData.singleOrNull() ?: return null
        val selectedLeaf =
            selectedStop.data.find { it.direction.id == stopFilter.directionId } ?: return null
        val trip =
            selectedLeaf.upcomingTrips.find { it.trip.id == previousFilters.tripFilter?.tripId }
        val destination = trip?.trip?.headsign ?: selectedLeaf.direction.destination

        return ScreenReaderContext(
            selectedRoute.lineOrRoute.type,
            destination,
            selectedStop.stop.name,
        )
    }

    public fun filterVehiclesByUpcoming(
        routeCardData: List<RouteCardData>,
        vehicles: VehiclesStreamDataResponse,
    ): Map<String, Vehicle> {
        val routeIds =
            routeCardData
                .asSequence()
                .flatMap { it.stopData }
                .flatMap { it.data }
                .flatMap { it.upcomingTrips }
                .map { it.routeId }
                .toSet()
        val filtered = vehicles.vehicles.filter { routeIds.contains(it.value.routeId) }
        return filtered
    }
}
