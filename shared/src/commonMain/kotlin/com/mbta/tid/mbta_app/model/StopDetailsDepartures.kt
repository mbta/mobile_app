package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.RealtimePatterns.ByHeadsign
import com.mbta.tid.mbta_app.model.RealtimePatterns.Companion.formatUpcomingTrip
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import kotlinx.datetime.Instant

data class TripAndFormat(
    val upcoming: UpcomingTrip,
    val formatted: RealtimePatterns.Format.Some.FormatWithId
)

data class StopDetailsDepartures(val routes: List<PatternsByStop>) {
    val allUpcomingTrips = routes.flatMap { it.allUpcomingTrips() }

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
        return StopDetailsFilter(route.routeIdentifier, direction)
    }

    fun autoTripFilter(
        stopFilter: StopDetailsFilter?,
        currentTripFilter: TripDetailsFilter?,
        filterAtTime: Instant
    ): TripDetailsFilter? {
        if (stopFilter == null) {
            return null
        }
        val relevantTrips =
            stopDetailsFormattedTrips(stopFilter.routeId, stopFilter.directionId, filterAtTime)
                .filter {
                    when (it.formatted.format) {
                        is TripInstantDisplay.Cancelled -> false
                        else -> true
                    }
                }
                .map { it.upcoming }

        if (
            currentTripFilter != null &&
                (relevantTrips.any { it.trip.id == currentTripFilter.tripId } ||
                    currentTripFilter.selectionLock)
        ) {
            return currentTripFilter
        }

        val firstTrip: UpcomingTrip = relevantTrips.firstOrNull() ?: return null
        return TripDetailsFilter(
            tripId = firstTrip.trip.id,
            vehicleId = firstTrip.vehicle?.id,
            stopSequence = firstTrip.stopSequence
        )
    }

    companion object {
        fun fromData(
            stopId: String,
            global: GlobalResponse,
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse?,
            alerts: AlertsStreamDataResponse?,
            pinnedRoutes: Set<String>,
            filterAtTime: Instant,
            useTripHeadsigns: Boolean,
        ): StopDetailsDepartures? {
            val stop = global.stops[stopId]
            return if (stop == null) {
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
                    useTripHeadsigns
                )
            }
        }

        fun fromData(
            stop: Stop,
            global: GlobalResponse,
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse?,
            alerts: AlertsStreamDataResponse?,
            pinnedRoutes: Set<String>,
            filterAtTime: Instant,
            useTripHeadsigns: Boolean,
        ): StopDetailsDepartures? {
            val allStopIds =
                if (global.patternIdsByStop.containsKey(stop.id)) {
                    listOf(stop.id)
                } else {
                    stop.childStopIds.filter { global.stops.containsKey(it) }
                }

            val staticData = NearbyStaticData(global, NearbyResponse(allStopIds))
            val routes =
                if (useTripHeadsigns) {
                        staticData.withRealtimeInfoViaTripHeadsigns(
                            global,
                            null,
                            schedules,
                            predictions,
                            alerts,
                            filterAtTime,
                            showAllPatternsWhileLoading = true,
                            hideNonTypicalPatternsBeyondNext = null,
                            filterCancellations = false,
                            pinnedRoutes
                        )
                    } else {
                        staticData.withRealtimeInfoWithoutTripHeadsigns(
                            global,
                            null,
                            schedules,
                            predictions,
                            alerts,
                            filterAtTime,
                            showAllPatternsWhileLoading = true,
                            hideNonTypicalPatternsBeyondNext = null,
                            filterCancellations = false,
                            pinnedRoutes
                        )
                    }
                    ?.flatMap { it.patternsByStop }

            return routes?.let { StopDetailsDepartures(it) }
        }

        fun getStatusDepartures(
            realtimePatterns: List<RealtimePatterns>,
            now: Instant
        ): List<StopDetailsStatusRowData> {
            return realtimePatterns.mapNotNull { pattern ->
                when (pattern) {
                    is ByHeadsign -> {
                        getStatusFormat(pattern, now)?.let {
                            StopDetailsStatusRowData(pattern.route, pattern.headsign, it)
                        }
                    }
                    else -> null
                }
            }
        }

        private fun getStatusFormat(pattern: ByHeadsign, now: Instant): RealtimePatterns.Format? {
            val noPredictions =
                pattern.upcomingTrips.any { it.time != null && it.time > now && !it.isCancelled }
            val hasTripsToShow =
                pattern.upcomingTrips.any {
                    formatUpcomingTrip(
                        now,
                        it,
                        pattern.route.type,
                        TripInstantDisplay.Context.StopDetailsFiltered,
                        pattern.route.type.isSubway()
                    ) != null
                }
            return when {
                hasTripsToShow || !pattern.allDataLoaded -> null
                noPredictions -> RealtimePatterns.Format.None(null)
                !pattern.hasSchedulesToday -> RealtimePatterns.Format.NoSchedulesToday(null)
                else -> RealtimePatterns.Format.ServiceEndedToday(null)
            }
        }
    }
}
