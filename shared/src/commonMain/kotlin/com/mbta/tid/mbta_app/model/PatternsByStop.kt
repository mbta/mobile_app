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
    val directions: List<Direction>,
    val elevatorAlerts: List<Alert>
) {
    val representativeRoute = routes.min()
    val routeIdentifier = line?.id ?: representativeRoute.id
    val position = Position(longitude = stop.longitude, latitude = stop.latitude)

    constructor(
        staticData: NearbyStaticData.StopPatterns,
        upcomingTripsMap: UpcomingTripsMap,
        patternsPredicate: (RealtimePatterns) -> Boolean,
        alerts: Collection<Alert>,
        tripsById: Map<String, Trip>,
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
                                Alert.applicableAlerts(
                                        alerts.toList(),
                                        null,
                                        listOf(it.route.id),
                                        it.stopIds,
                                        null
                                    )
                                    .filterNot {
                                        it.effect == Alert.Effect.TrackChange &&
                                            staticData.stop.isCRCore
                                    },
                                alertsDownstream(
                                    alerts.toList(),
                                    it.patterns,
                                    it.stopIds,
                                    tripsById
                                ),
                                hasSchedulesTodayByPattern,
                                allDataLoaded
                            )
                        )
                    is NearbyStaticData.StaticPatterns.ByDirection ->
                        resolveRealtimePatternForDirection(
                            it,
                            upcomingTripsMap,
                            staticData.stop.id,
                            alerts.filterNot {
                                it.effect == Alert.Effect.TrackChange && staticData.stop.isCRCore
                            },
                            tripsById,
                            hasSchedulesTodayByPattern,
                            allDataLoaded
                        )
                }
            }
            .filter(patternsPredicate)
            .sortedWith(PatternSorting.compareRealtimePatterns()),
        staticData.directions,
        Alert.elevatorAlerts(alerts, setOf(staticData.stop.id))
    )

    constructor(
        lineOrRoute: NearbyHierarchy.LineOrRoute,
        stop: Stop,
        directions: List<Direction>,
        stopData: Map<NearbyHierarchy.DirectionOrHeadsign, NearbyHierarchy.NearbyLeaf>,
        patternsPredicate: (RealtimePatterns) -> Boolean,
        alerts: Collection<Alert>,
        tripsById: Map<String, Trip>,
        hasSchedulesTodayByPattern: Map<String, Boolean>?,
        allDataLoaded: Boolean,
    ) : this(
        when (lineOrRoute) {
            is NearbyHierarchy.LineOrRoute.Route -> listOf(lineOrRoute.route)
            is NearbyHierarchy.LineOrRoute.Line -> lineOrRoute.routes.sorted()
        },
        when (lineOrRoute) {
            is NearbyHierarchy.LineOrRoute.Route -> null
            is NearbyHierarchy.LineOrRoute.Line -> lineOrRoute.line
        },
        stop,
        stopData
            .flatMap { (directionOrHeadsign, leaf) ->
                when (directionOrHeadsign) {
                    is NearbyHierarchy.DirectionOrHeadsign.Headsign ->
                        listOf(
                            RealtimePatterns.ByHeadsign(
                                directionOrHeadsign,
                                leaf,
                                Alert.applicableAlerts(
                                        alerts.toList(),
                                        null,
                                        listOf(directionOrHeadsign.route.id),
                                        leaf.childStopIds,
                                        null
                                    )
                                    .filterNot {
                                        it.effect == Alert.Effect.TrackChange && stop.isCRCore
                                    },
                                alertsDownstream(
                                    alerts.toList(),
                                    leaf.routePatterns.filterNotNull(),
                                    leaf.childStopIds,
                                    tripsById
                                ),
                                hasSchedulesTodayByPattern,
                                allDataLoaded
                            )
                        )
                    is NearbyHierarchy.DirectionOrHeadsign.Direction ->
                        resolveRealtimePatternForDirection(
                            checkNotNull(lineOrRoute as? NearbyHierarchy.LineOrRoute.Line),
                            directionOrHeadsign,
                            leaf,
                            alerts.filterNot {
                                it.effect == Alert.Effect.TrackChange && stop.isCRCore
                            },
                            tripsById,
                            hasSchedulesTodayByPattern,
                            allDataLoaded
                        )
                }
            }
            .filter(patternsPredicate)
            .sortedWith(PatternSorting.compareRealtimePatterns()),
        directions,
        Alert.elevatorAlerts(alerts, setOf(stop.id))
    )

    constructor(
        route: Route,
        stop: Stop,
        patterns: List<RealtimePatterns>,
        elevatorAlerts: List<Alert> = emptyList()
    ) : this(
        listOf(route),
        null,
        stop,
        patterns,
        listOf(Direction(0, route), Direction(1, route)),
        elevatorAlerts
    )

    @OptIn(ExperimentalTurfApi::class)
    fun distanceFrom(position: Position) = distance(position, this.position)

    fun allUpcomingTrips(): List<UpcomingTrip> = this.patterns.flatMap { it.upcomingTrips }.sorted()

    fun alertsHereFor(directionId: Int, tripId: String?, global: GlobalResponse): List<Alert> {
        val patternsInDirection = this.patterns.filter { it.directionId() == directionId }
        val stopIds = arrayOf(this.stop.id) + this.stop.childStopIds
        val stopsInDirection =
            patternsInDirection
                .flatMap { realtime ->
                    realtime.patterns.mapNotNull { pattern ->
                        stopIds.firstOrNull {
                            global.trips[pattern?.representativeTripId]?.stopIds?.contains(it)
                                ?: false
                        }
                    }
                }
                .toSet()
        return stopsInDirection
            .flatMap { stopId ->
                patternsInDirection
                    .flatMap { it.alertsHereFor(setOf(stopId), directionId, tripId) ?: emptyList() }
                    .toSet()
            }
            .distinct()
    }

    /*
    Whether the upcoming trip with the given id is cancelled
     */
    fun tripIsCancelled(tripId: String): Boolean {
        return this.patterns.any {
            it.upcomingTrips.any { trip -> trip.trip.id == tripId && trip.isCancelled }
        }
    }

    /** get the alerts downstream of the patterns in the given direction */
    fun alertsDownstream(directionId: Int): List<Alert> {
        val patternsInDirection = this.patterns.filter { it.directionId() == directionId }
        return patternsInDirection.flatMap { it.alertsDownstream ?: emptyList() }.distinct()
    }

    companion object {
        /**
         * A unique list of all the alerts that are downstream from the target stop for each route
         * pattern
         */
        fun alertsDownstream(
            alerts: Collection<Alert>,
            patterns: List<RoutePattern>,
            targetStopWithChildren: Set<String>,
            tripsById: Map<String, Trip>
        ): List<Alert> {
            return patterns
                .flatMap {
                    val trip = tripsById[it.representativeTripId]
                    if (trip != null) {
                        Alert.downstreamAlerts(alerts, trip, targetStopWithChildren)
                    } else {
                        listOf()
                    }
                }
                .distinct()
        }

        // Even if a direction can serve multiple routes according to the static data, it's possible
        // that only one of those routes is typical, in which case we don't want to display it as a
        // grouped direction in the UI. If there are only predicted trips on a single route, this
        // will split the grouped direction up into as many headsign rows as there are headsigns.
        fun resolveRealtimePatternForDirection(
            staticData: NearbyStaticData.StaticPatterns.ByDirection,
            upcomingTripsMap: UpcomingTripsMap,
            parentStopId: String,
            alerts: Collection<Alert>,
            tripsById: Map<String, Trip>,
            hasSchedulesTodayByPattern: Map<String, Boolean>?,
            allDataLoaded: Boolean,
        ): List<RealtimePatterns> {
            val typicalPatternsByRoute = getTypicalPatternsByRoute(staticData)

            // If data hasn't loaded, or there are more than one typical routes in this direction,
            // we never want to split into individual headsign rows.
            if (!allDataLoaded || typicalPatternsByRoute.size > 1) {
                return listOf(
                    RealtimePatterns.ByDirection(
                        staticData,
                        upcomingTripsMap,
                        parentStopId,
                        Alert.applicableAlerts(
                            alerts.toList(),
                            null,
                            staticData.routeIds,
                            staticData.stopIds,
                            null
                        ),
                        alertsDownstream(
                            alerts.toList(),
                            staticData.patterns,
                            staticData.stopIds,
                            tripsById
                        ),
                        hasSchedulesTodayByPattern,
                        allDataLoaded
                    )
                )
            }

            val patternsById = staticData.patterns.associateBy { it.id }
            val headsignsAndPatternsToDisplayByRoute =
                getHeadsignsAndPatternsToDisplayByRoute(
                    staticData,
                    upcomingTripsMap,
                    parentStopId,
                    patternsById,
                    typicalPatternsByRoute
                )

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
                        Alert.applicableAlerts(
                            alerts.toList(),
                            null,
                            staticData.routeIds,
                            staticData.stopIds,
                            null
                        ),
                        alertsDownstream(
                            alerts.toList(),
                            staticData.patterns,
                            staticData.stopIds,
                            tripsById
                        ),
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
                    Alert.applicableAlerts(
                        alerts.toList(),
                        null,
                        staticData.routeIds,
                        staticData.stopIds,
                        null
                    ),
                    alertsDownstream(
                        alerts.toList(),
                        staticData.patterns,
                        staticData.stopIds,
                        tripsById
                    ),
                    hasSchedulesTodayByPattern,
                    allDataLoaded
                )
            )
        }

        fun resolveRealtimePatternForDirection(
            line: NearbyHierarchy.LineOrRoute.Line,
            direction: NearbyHierarchy.DirectionOrHeadsign.Direction,
            leaf: NearbyHierarchy.NearbyLeaf,
            alerts: Collection<Alert>,
            tripsById: Map<String, Trip>,
            hasSchedulesTodayByPattern: Map<String, Boolean>?,
            allDataLoaded: Boolean,
        ): List<RealtimePatterns> {
            val typicalPatternsByRoute = getTypicalPatternsByRoute(leaf)

            val alertsHere =
                Alert.applicableAlerts(
                    alerts,
                    null,
                    line.routes.map { it.id },
                    leaf.childStopIds,
                    null
                )
            val alertsDownstream =
                alertsDownstream(
                    alerts.toList(),
                    leaf.routePatterns.filterNotNull(),
                    leaf.childStopIds,
                    tripsById
                )

            // If data hasn't loaded, or there are more than one typical routes in this direction,
            // we never want to split into individual headsign rows.
            if (!allDataLoaded || typicalPatternsByRoute.size > 1) {
                return listOf(
                    RealtimePatterns.ByDirection(
                        line,
                        direction,
                        leaf,
                        alertsHere,
                        alertsDownstream,
                        hasSchedulesTodayByPattern,
                        allDataLoaded
                    )
                )
            }

            val headsignsAndPatternsToDisplayByRoute =
                getHeadsignsAndPatternsToDisplayByRoute(leaf, typicalPatternsByRoute)

            // If there is only a single route with predicted or typical trips, we want to break up
            // the grouped direction instead into individual headsign rows.
            val firstDisplayedRoute =
                line.routes.firstOrNull {
                    it.id == headsignsAndPatternsToDisplayByRoute.keys.firstOrNull()
                }
            if (headsignsAndPatternsToDisplayByRoute.size == 1 && firstDisplayedRoute != null) {
                val headsignsToDisplay =
                    (headsignsAndPatternsToDisplayByRoute[firstDisplayedRoute.id] ?: emptyMap())
                return headsignsToDisplay.map { (headsign, patternIds) ->
                    RealtimePatterns.ByHeadsign(
                        NearbyHierarchy.DirectionOrHeadsign.Headsign(
                            headsign,
                            firstDisplayedRoute,
                            line.line
                        ),
                        leaf.filterPatterns { it?.id in patternIds },
                        alertsHere,
                        alertsDownstream,
                        hasSchedulesTodayByPattern,
                        allDataLoaded
                    )
                }
            }

            return listOf(
                RealtimePatterns.ByDirection(
                    line,
                    direction,
                    leaf,
                    alertsHere,
                    alertsDownstream,
                    hasSchedulesTodayByPattern,
                    allDataLoaded
                )
            )
        }

        private fun getHeadsignsAndPatternsToDisplayByRoute(
            staticData: NearbyStaticData.StaticPatterns.ByDirection,
            upcomingTripsMap: UpcomingTripsMap?,
            parentStopId: String,
            patternsById: Map<String, RoutePattern>,
            typicalPatternsByRoute: Map<String, List<String>>,
        ): Map<String, Map<String, Set<String?>>> {
            val tripsByPattern = getTripsByPattern(upcomingTripsMap, patternsById, parentStopId)
            val predictedPatternsByRoute = getPredictedPatternsByRoute(staticData, tripsByPattern)
            val upcomingPatternsByRouteAndHeadsign =
                tripsByPattern.values
                    .flatten()
                    .groupBy({ it.trip.routeId }, { it.trip.headsign to it.trip.routePatternId })
                    .mapValues { routeEntry ->
                        routeEntry.value.groupBy({ it.first }, { it.second })
                    }

            // We don't have access to global data or representative trips here, so the only way
            // that we can determine headsigns is by looking at the actual upcoming trips.
            return upcomingPatternsByRouteAndHeadsign
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
        }

        private fun getHeadsignsAndPatternsToDisplayByRoute(
            leaf: NearbyHierarchy.NearbyLeaf,
            typicalPatternsByRoute: Map<String, List<String>>,
        ): Map<String, Map<String, Set<String?>>> {
            val tripsByPattern = getTripsByPattern(leaf)
            val predictedPatternsByRoute = getPredictedPatternsByRoute(leaf, tripsByPattern)
            val upcomingPatternsByRouteAndHeadsign =
                tripsByPattern.values
                    .flatten()
                    .groupBy({ it.trip.routeId }, { it.trip.headsign to it.trip.routePatternId })
                    .mapValues { routeEntry ->
                        routeEntry.value.groupBy({ it.first }, { it.second })
                    }

            // We don't have access to global data or representative trips here, so the only way
            // that we can determine headsigns is by looking at the actual upcoming trips.
            return upcomingPatternsByRouteAndHeadsign
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
        }

        private fun getPredictedPatternsByRoute(
            staticData: NearbyStaticData.StaticPatterns.ByDirection,
            tripsByPattern: Map<String, List<UpcomingTrip>>
        ): Map<String, List<String>> {
            return staticData.patterns
                .filter { tripsByPattern[it.id]?.any { trip -> trip.prediction != null } == true }
                .groupBy({ it.routeId }, { it.id })
        }

        private fun getPredictedPatternsByRoute(
            leaf: NearbyHierarchy.NearbyLeaf,
            tripsByPattern: Map<String?, List<UpcomingTrip>>
        ): Map<String, List<String?>> {
            return leaf.routePatterns
                .filter { tripsByPattern[it?.id]?.any { trip -> trip.prediction != null } == true }
                .groupBy(
                    { pattern ->
                        pattern?.routeId
                            ?: leaf.upcomingTrips
                                .first { it.trip.routePatternId == pattern?.id }
                                .trip
                                .routeId
                    },
                    { it?.id }
                )
        }

        private fun getTripsByPattern(
            upcomingTripsMap: UpcomingTripsMap?,
            patternsById: Map<String, RoutePattern>,
            parentStopId: String,
        ): Map<String, List<UpcomingTrip>> {
            return upcomingTripsMap
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
        }

        private fun getTripsByPattern(
            leaf: NearbyHierarchy.NearbyLeaf,
        ): Map<String?, List<UpcomingTrip>> {
            return leaf.upcomingTrips.groupBy { it.trip.routePatternId }
        }

        private fun getTypicalPatternsByRoute(
            staticData: NearbyStaticData.StaticPatterns.ByDirection
        ): Map<String, List<String>> {
            return staticData.patterns
                .filter { it.typicality == RoutePattern.Typicality.Typical }
                .groupBy({ it.routeId }, { it.id })
        }

        private fun getTypicalPatternsByRoute(
            leaf: NearbyHierarchy.NearbyLeaf
        ): Map<String, List<String>> {
            return leaf.routePatterns
                .filterNotNull()
                .filter { it.typicality == RoutePattern.Typicality.Typical }
                .groupBy({ it.routeId }, { it.id })
        }
    }
}
