package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.RealtimePatterns.Companion.formatUpcomingTrip
import com.mbta.tid.mbta_app.model.UpcomingFormat.NoTripsFormat
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.TileData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

// This is no longer necessary now that FormattedTrip includes the UpcomingTrip used to create it,
// but it's left here for backwards compatibility until we replace StopDetailsDepartures
data class TripAndFormat(
    val upcoming: UpcomingTrip,
    val formatted: UpcomingFormat.Some.FormattedTrip
)

data class StopDetailsDepartures(val routes: List<PatternsByStop>) {
    val allUpcomingTrips = routes.flatMap { it.allUpcomingTrips() }
    val elevatorAlerts = routes.flatMap { it.elevatorAlerts }.distinct()

    val upcomingPatternIds = allUpcomingTrips.mapNotNull { it.trip.routePatternId }.toSet()

    fun filterVehiclesByUpcoming(vehicles: VehiclesStreamDataResponse): Map<String, Vehicle> {
        val routeIds = allUpcomingTrips.map { it.trip.routeId }.toSet()
        val filtered = vehicles.vehicles.filter { routeIds.contains(it.value.routeId) }
        return filtered
    }

    fun stopDetailsFormattedTrips(
        routeId: String,
        directionId: Int,
        filterAtTime: Instant
    ): List<TripAndFormat> {
        val patternsByStop =
            routes.firstOrNull { it.routeIdentifier == routeId } ?: return emptyList()
        val trips =
            patternsByStop
                .allUpcomingTrips()
                .filter { it.trip.directionId == directionId }
                .mapNotNull {
                    val route =
                        patternsByStop.routes.firstOrNull { route -> it.trip.routeId == route.id }
                            ?: return@mapNotNull null
                    val format =
                        formatUpcomingTrip(
                            filterAtTime,
                            it,
                            route.type,
                            TripInstantDisplay.Context.StopDetailsFiltered
                        )
                            ?: return@mapNotNull null

                    TripAndFormat(it, format)
                }
        return trips
    }

    fun tileData(
        routeId: String,
        directionId: Int,
        filterAtTime: Instant,
        globalData: GlobalResponse?
    ): List<TileData> =
        stopDetailsFormattedTrips(routeId, directionId, filterAtTime).mapNotNull { tripAndFormat ->
            val upcoming = tripAndFormat.upcoming
            val route = globalData?.getRoute(upcoming.trip.routeId)
            if (route == null) {
                println("Failed to find route ID ${upcoming.trip.routeId} from upcoming trip")
                null
            } else {
                TileData.fromUpcoming(upcoming, route, filterAtTime)
            }
        }

    /**
     * If the stop serves only 1 route in a single direction, returns a new filter for that route
     * and direction.
     */
    fun autoStopFilter(): StopDetailsFilter? {
        if (routes.size != 1) {
            return null
        }
        val route = routes.first()
        val directions = route.patterns.map { it.directionId() }.toSet()
        if (directions.size != 1) {
            return null
        }
        val direction = directions.first()
        return StopDetailsFilter(route.routeIdentifier, direction, autoFilter = true)
    }

    fun autoTripFilter(
        stopFilter: StopDetailsFilter?,
        currentTripFilter: TripDetailsFilter?,
        filterAtTime: Instant
    ): TripDetailsFilter? {
        if (stopFilter == null) {
            return null
        }

        if (currentTripFilter?.selectionLock == true) return currentTripFilter

        val relevantTrips =
            stopDetailsFormattedTrips(stopFilter.routeId, stopFilter.directionId, filterAtTime)
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

    class ScreenReaderContext(
        val routeType: RouteType,
        val destination: String?,
        val stopName: String
    )

    /*
    Whether the upcoming trip with the given id is cancelled in the upcoming departures
     */
    fun tripIsCancelled(tripId: String): Boolean {
        return this.routes.any { it.tripIsCancelled(tripId) }
    }

    fun getScreenReaderTripDepartureContext(
        previousFilters: StopDetailsPageFilters
    ): ScreenReaderContext? {
        val stopFilter = previousFilters.stopFilter ?: return null
        val selectedPattern =
            this.routes.firstOrNull { it.routeIdentifier == stopFilter.routeId } ?: return null
        val trip = allUpcomingTrips.firstOrNull { it.trip.id == previousFilters.tripFilter?.tripId }
        val destination =
            trip?.trip?.headsign ?: selectedPattern.directions[stopFilter.directionId].destination

        return ScreenReaderContext(
            selectedPattern.representativeRoute.type,
            destination,
            selectedPattern.stop.name
        )
    }

    companion object {
        @DefaultArgumentInterop.Enabled
        suspend fun fromData(
            stopId: String,
            global: GlobalResponse,
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse?,
            alerts: AlertsStreamDataResponse?,
            pinnedRoutes: Set<String>,
            filterAtTime: Instant,
            dispatcher: CoroutineDispatcher = Dispatchers.Default
        ): StopDetailsDepartures? =
            withContext(dispatcher) {
                val stop = global.stops[stopId]
                if (stop == null) {
                    null
                } else {
                    fromData(
                        stop,
                        global,
                        schedules,
                        predictions,
                        alerts,
                        pinnedRoutes,
                        filterAtTime,
                        dispatcher
                    )
                }
            }

        @DefaultArgumentInterop.Enabled
        suspend fun fromData(
            stop: Stop,
            global: GlobalResponse,
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse?,
            alerts: AlertsStreamDataResponse?,
            pinnedRoutes: Set<String>,
            filterAtTime: Instant,
            dispatcher: CoroutineDispatcher = Dispatchers.Default
        ): StopDetailsDepartures? =
            withContext(dispatcher) {
                val allStopIds =
                    if (global.patternIdsByStop.containsKey(stop.id)) {
                        listOf(stop.id)
                    } else {
                        stop.childStopIds.filter { global.stops.containsKey(it) }
                    }

                val staticData = NearbyStaticData(global, NearbyResponse(allStopIds))
                val routes =
                    staticData
                        .withRealtimeInfoWithoutTripHeadsigns(
                            global,
                            null,
                            schedules,
                            predictions,
                            alerts,
                            filterAtTime,
                            showAllPatternsWhileLoading = true,
                            hideNonTypicalPatternsBeyondNext = null,
                            filterCancellations = false,
                            includeMinorAlerts = true,
                            pinnedRoutes
                        )
                        ?.flatMap { it.patternsByStop }

                routes?.let { StopDetailsDepartures(it) }
            }

        fun getNoPredictionsStatus(
            realtimePatterns: List<RealtimePatterns>,
            now: Instant
        ): NoTripsFormat? {
            val patternStatuses =
                realtimePatterns.mapNotNull { pattern -> getStatusFormat(pattern, now) }

            return if (patternStatuses.isEmpty() || patternStatuses.size != realtimePatterns.size) {
                null
            } else if (patternStatuses.all { patternStatuses.first()::class == it::class }) {
                patternStatuses.first()
            } else if (
                patternStatuses.all {
                    when (it) {
                        is NoTripsFormat.NoSchedulesToday -> true
                        is NoTripsFormat.ServiceEndedToday -> true
                        else -> false
                    }
                }
            ) {
                // If there's a mixture of no service today and service ended, but nothing else,
                // service ended takes precedence
                NoTripsFormat.ServiceEndedToday
            } else {
                NoTripsFormat.PredictionsUnavailable
            }
        }

        fun getStatusDepartures(
            realtimePatterns: List<RealtimePatterns>,
            now: Instant
        ): List<StopDetailsStatusRowData> {
            return realtimePatterns.mapNotNull { pattern ->
                when (pattern) {
                    is RealtimePatterns.ByHeadsign -> {
                        getStatusFormat(pattern, now)?.let {
                            StopDetailsStatusRowData(
                                pattern.route,
                                pattern.headsign,
                                UpcomingFormat.NoTrips(it)
                            )
                        }
                    }
                    else -> null
                }
            }
        }

        private fun getStatusFormat(pattern: RealtimePatterns, now: Instant): NoTripsFormat? {
            val routeType =
                when (pattern) {
                    is RealtimePatterns.ByDirection -> pattern.representativeRoute.type
                    is RealtimePatterns.ByHeadsign -> pattern.route.type
                }
            val hasTripsToShow =
                pattern.upcomingTrips.any {
                    formatUpcomingTrip(
                        now,
                        it,
                        routeType,
                        TripInstantDisplay.Context.StopDetailsFiltered
                    ) != null
                }
            return if (hasTripsToShow || !pattern.allDataLoaded) {
                null
            } else {
                NoTripsFormat.fromUpcomingTrips(
                    pattern.upcomingTrips,
                    pattern.hasSchedulesToday,
                    now
                )
            }
        }
    }
}
