package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import io.github.dellisd.spatialk.geojson.Position
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import io.github.dellisd.spatialk.turf.distance

/**
 * @property patterns [RealtimePatterns]s serving the stop grouped by headsign or direction. The
 *   destinations are listed in ascending order based on [RoutePattern.sortOrder]
 */
data class PatternsByStop(
    val routes: List<Route>,
    val line: Line?,
    val stop: Stop,
    val patterns: List<RealtimePatterns>,
    val directions: List<Direction>
) {
    val representativeRoute = routes.min()
    val routeIdentifier = line?.id ?: representativeRoute.id
    val position = Position(longitude = stop.longitude, latitude = stop.latitude)

    constructor(
        staticData: NearbyStaticData.StopPatterns,
        upcomingTripsMap: UpcomingTripsMap?,
        patternsPredicate: (RealtimePatterns) -> Boolean,
        alerts: Collection<Alert>?,
        hasSchedulesTodayByPattern: Map<String, Boolean>?,
        allDataLoaded: Boolean,
    ) : this(
        when (staticData) {
            is NearbyStaticData.StopPatterns.ForRoute -> listOf(staticData.route)
            is NearbyStaticData.StopPatterns.ForLine -> staticData.routes
        },
        when (staticData) {
            is NearbyStaticData.StopPatterns.ForRoute -> null
            is NearbyStaticData.StopPatterns.ForLine -> staticData.line
        },
        staticData.stop,
        staticData.patterns
            .flatMap {
                when (it) {
                    is NearbyStaticData.StaticPatterns.ByHeadsign ->
                        listOf(
                            RealtimePatterns.ByHeadsign(
                                it,
                                upcomingTripsMap,
                                staticData.stop.id,
                                alerts,
                                hasSchedulesTodayByPattern,
                                allDataLoaded
                            )
                        )
                    is NearbyStaticData.StaticPatterns.ByDirection ->
                        resolveRealtimePatternForDirection(
                            it,
                            upcomingTripsMap,
                            staticData.stop.id,
                            alerts,
                            hasSchedulesTodayByPattern,
                            allDataLoaded
                        )
                }
            }
            .filter(patternsPredicate)
            .sortedWith(PatternSorting.compareRealtimePatterns()),
        staticData.directions
    )

    constructor(
        route: Route,
        stop: Stop,
        patterns: List<RealtimePatterns>
    ) : this(listOf(route), null, stop, patterns, listOf(Direction(0, route), Direction(1, route)))

    @OptIn(ExperimentalTurfApi::class)
    fun distanceFrom(position: Position) = distance(position, this.position)

    fun allUpcomingTrips(): List<UpcomingTrip> =
        this.patterns.flatMap { it.upcomingTrips ?: emptyList() }.sorted()

    fun alertsHereFor(directionId: Int, global: GlobalResponse): List<Alert> {
        val patternsInDirection = this.patterns.filter { it.directionId() == directionId }
        val stopIds = arrayOf(this.stop.id) + this.stop.childStopIds
        val stopsInDirection =
            patternsInDirection
                .flatMap { realtime ->
                    realtime.patterns.mapNotNull { pattern ->
                        stopIds.firstOrNull {
                            global.trips[pattern.representativeTripId]?.stopIds?.contains(it)
                                ?: false
                        }
                    }
                }
                .toSet()
        return stopsInDirection
            .flatMap { stopId ->
                patternsInDirection
                    .flatMap { it.alertsHereFor(setOf(stopId), directionId) ?: emptyList() }
                    .toSet()
            }
            .distinct()
    }

    companion object {
        // Even if a direction can serve multiple routes according to the static data, it's possible
        // that only one of those routes is typical, in which case we don't want to display it as a
        // grouped direction in the UI. If there are only predicted trips on a single route, this
        // will split the grouped direction up into as many headsign rows as there are headsigns.
        fun resolveRealtimePatternForDirection(
            staticData: NearbyStaticData.StaticPatterns.ByDirection,
            upcomingTripsMap: UpcomingTripsMap?,
            parentStopId: String,
            alerts: Collection<Alert>?,
            hasSchedulesTodayByPattern: Map<String, Boolean>?,
            allDataLoaded: Boolean,
        ): List<RealtimePatterns> {
            val typicalPatternsByRoute =
                staticData.patterns
                    .filter { it.typicality == RoutePattern.Typicality.Typical }
                    .groupBy({ it.routeId }, { it.id })

            // If data hasn't loaded, or there are more than one typical routes in this direction,
            // we never want to split into individual headsign rows.
            if (!allDataLoaded || typicalPatternsByRoute.size > 1) {
                return listOf(
                    RealtimePatterns.ByDirection(
                        staticData,
                        upcomingTripsMap,
                        parentStopId,
                        alerts,
                        hasSchedulesTodayByPattern,
                        allDataLoaded
                    )
                )
            }

            val patternsById = staticData.patterns.associateBy { it.id }
            val tripsByPattern =
                upcomingTripsMap
                    .orEmpty()
                    .mapNotNull {
                        when (val key = it.key) {
                            is RealtimePatterns.UpcomingTripKey.ByRoutePattern ->
                                if (
                                    key.routePatternId != null &&
                                        patternsById.containsKey(key.routePatternId) &&
                                        key.parentStopId == parentStopId
                                ) {
                                    key.routePatternId to it.value
                                } else {
                                    null
                                }
                            else -> null
                        }
                    }
                    .toMap()
            val upcomingPatternsByRouteAndHeadsign =
                tripsByPattern.values
                    .flatten()
                    .groupBy({ it.trip.routeId }, { it.trip.headsign to it.trip.routePatternId })
                    .mapValues { routeEntry ->
                        routeEntry.value.groupBy({ it.first }, { it.second })
                    }

            val predictedPatternsByRoute =
                staticData.patterns
                    .filter {
                        tripsByPattern[it.id]?.any { trip ->
                            trip.prediction != null && trip.vehicle?.tripId == trip.trip.id
                        } == true
                    }
                    .groupBy({ it.routeId }, { it.id })

            // We don't have access to global data or representative trips here, so the only way
            // that we can determine headsigns is by looking at the actual upcoming trips.
            val headsignsAndPatternsToDisplayByRoute =
                upcomingPatternsByRouteAndHeadsign
                    .mapNotNull {
                        val headsignsToDisplay =
                            it.value
                                .mapNotNull { headsignsToPatterns ->
                                    val patternsToDisplay =
                                        (typicalPatternsByRoute[it.key].orEmpty() +
                                                predictedPatternsByRoute[it.key].orEmpty())
                                            .toSet()
                                            .intersect(headsignsToPatterns.value.toSet())
                                    if (patternsToDisplay.isEmpty()) {
                                        null
                                    } else {
                                        headsignsToPatterns.key to patternsToDisplay
                                    }
                                }
                                .toMap()
                        if (headsignsToDisplay.isEmpty()) {
                            null
                        } else {
                            it.key to headsignsToDisplay
                        }
                    }
                    .toMap()

            // If there is only a single route with predicted or typical trips, we want to break up
            // the grouped direction instead into individual headsign rows.
            val firstDisplayedRoute =
                staticData.routes.firstOrNull {
                    it.id == headsignsAndPatternsToDisplayByRoute.keys.firstOrNull()
                }
            if (headsignsAndPatternsToDisplayByRoute.size == 1 && firstDisplayedRoute != null) {
                val headsignsToDisplay =
                    (headsignsAndPatternsToDisplayByRoute[firstDisplayedRoute.id] ?: emptyMap())
                return headsignsToDisplay.map { (headsign, patternIds) ->
                    val patternsForHeadsign = patternIds.mapNotNull { patternsById[it] }
                    RealtimePatterns.ByHeadsign(
                        NearbyStaticData.StaticPatterns.ByHeadsign(
                            firstDisplayedRoute,
                            headsign,
                            staticData.line,
                            patternsForHeadsign,
                            staticData.stopIds
                        ),
                        upcomingTripsMap,
                        parentStopId,
                        alerts,
                        hasSchedulesTodayByPattern,
                        allDataLoaded
                    )
                }
            }

            return listOf(
                RealtimePatterns.ByDirection(
                    staticData,
                    upcomingTripsMap,
                    parentStopId,
                    alerts,
                    hasSchedulesTodayByPattern,
                    allDataLoaded
                )
            )
        }
    }
}
