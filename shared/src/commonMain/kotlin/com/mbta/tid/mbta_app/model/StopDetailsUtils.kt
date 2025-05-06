package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.RouteCardData.Context
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import kotlinx.datetime.Instant

object StopDetailsUtils {
    /**
     * If the stop serves only 1 route in a single direction, returns a new filter for that route
     * and direction.
     */
    fun autoStopFilter(departures: StopDetailsDepartures?): StopDetailsFilter? {
        if (departures?.routes?.size != 1) {
            return null
        }
        val route = departures.routes.first()
        val directions = route.patterns.map { it.directionId() }.toSet()
        if (directions.size != 1) {
            return null
        }
        val direction = directions.first()
        return StopDetailsFilter(route.routeIdentifier, direction, autoFilter = true)
    }

    /**
     * If the stop serves only 1 route in a single direction, returns a new filter for that route
     * and direction.
     */
    fun autoStopFilter(routeCardData: List<RouteCardData>?): StopDetailsFilter? {
        val route = routeCardData?.singleOrNull() ?: return null
        val directions = route.stopData.flatMap { it.data }.map { it.directionId }.toSet()
        if (directions.size != 1) {
            return null
        }
        val direction = directions.first()
        return StopDetailsFilter(route.lineOrRoute.id, direction, autoFilter = true)
    }

    fun autoTripFilter(
        departures: StopDetailsDepartures?,
        stopFilter: StopDetailsFilter?,
        currentTripFilter: TripDetailsFilter?,
        filterAtTime: Instant
    ): TripDetailsFilter? {
        if (departures == null || stopFilter == null) {
            return null
        }

        if (currentTripFilter?.selectionLock == true) return currentTripFilter

        val relevantTrips =
            departures
                .stopDetailsFormattedTrips(stopFilter.routeId, stopFilter.directionId, filterAtTime)
                .map { it.upcoming }

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
            stopSequence = filterTrip.stopSequence
        )
    }

    fun autoTripFilter(
        routeCardData: List<RouteCardData>?,
        stopFilter: StopDetailsFilter?,
        currentTripFilter: TripDetailsFilter?,
        filterAtTime: Instant,
        globalData: GlobalResponse?
    ): TripDetailsFilter? {
        val route = routeCardData?.find { it.lineOrRoute.id == stopFilter?.routeId } ?: return null
        val leaf =
            route.stopData.singleOrNull()?.data?.find { it.directionId == stopFilter?.directionId }
                ?: return null
        if (currentTripFilter?.selectionLock == true) return currentTripFilter

        val leafFormat =
            leaf.format(
                filterAtTime,
                route.lineOrRoute.sortRoute,
                globalData,
                Context.StopDetailsFiltered
            )
        val formats =
            when (leafFormat) {
                is LeafFormat.Single -> listOf(leafFormat.format)
                is LeafFormat.Branched -> leafFormat.branchRows.map { it.format }
            }
        val relevantTrips =
            formats.flatMap { format ->
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
            stopSequence = filterTrip.stopSequence
        )
    }

    class ScreenReaderContext(
        val routeType: RouteType,
        val destination: String?,
        val stopName: String
    )

    fun getScreenReaderTripDepartureContext(
        departures: StopDetailsDepartures?,
        previousFilters: StopDetailsPageFilters
    ): ScreenReaderContext? {
        val stopFilter = previousFilters.stopFilter ?: return null
        val selectedPattern =
            departures?.routes?.firstOrNull { it.routeIdentifier == stopFilter.routeId }
                ?: return null
        val trip =
            departures.allUpcomingTrips.firstOrNull {
                it.trip.id == previousFilters.tripFilter?.tripId
            }
        val destination =
            trip?.trip?.headsign ?: selectedPattern.directions[stopFilter.directionId].destination

        return ScreenReaderContext(
            selectedPattern.representativeRoute.type,
            destination,
            selectedPattern.stop.name
        )
    }

    fun getScreenReaderTripDepartureContext(
        routeCardData: List<RouteCardData>?,
        previousFilters: StopDetailsPageFilters
    ): ScreenReaderContext? {
        val stopFilter = previousFilters.stopFilter ?: return null
        val selectedRoute =
            routeCardData?.firstOrNull { it.lineOrRoute.id == stopFilter.routeId } ?: return null
        val selectedStop = selectedRoute.stopData.singleOrNull() ?: return null
        val trip =
            selectedStop.data
                .flatMap { it.upcomingTrips }
                .find { it.trip.id == previousFilters.tripFilter?.tripId }
        val destination =
            trip?.trip?.headsign ?: selectedStop.directions[stopFilter.directionId].destination

        return ScreenReaderContext(
            selectedRoute.lineOrRoute.type,
            destination,
            selectedStop.stop.name
        )
    }

    fun filterVehiclesByUpcoming(
        departures: StopDetailsDepartures,
        vehicles: VehiclesStreamDataResponse
    ): Map<String, Vehicle> {
        val routeIds = departures.allUpcomingTrips.map { it.trip.routeId }.toSet()
        val filtered = vehicles.vehicles.filter { routeIds.contains(it.value.routeId) }
        return filtered
    }

    fun filterVehiclesByUpcoming(
        routeCardData: List<RouteCardData>,
        vehicles: VehiclesStreamDataResponse
    ): Map<String, Vehicle> {
        val routeIds =
            routeCardData
                .asSequence()
                .flatMap { it.stopData }
                .flatMap { it.data }
                .flatMap { it.upcomingTrips }
                .map { it.trip.routeId }
                .toSet()
        val filtered = vehicles.vehicles.filter { routeIds.contains(it.value.routeId) }
        return filtered
    }
}
