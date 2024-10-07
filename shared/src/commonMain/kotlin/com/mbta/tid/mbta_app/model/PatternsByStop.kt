package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import io.github.dellisd.spatialk.geojson.Position
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import io.github.dellisd.spatialk.turf.distance
import kotlinx.datetime.Instant

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
        filterTime: Instant,
        cutoffTime: Instant,
        alerts: Collection<Alert>?,
        hasSchedulesTodayByPattern: Map<String, Boolean>?,
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
            .map {
                when (it) {
                    is NearbyStaticData.StaticPatterns.ByHeadsign ->
                        RealtimePatterns.ByHeadsign(
                            it,
                            upcomingTripsMap,
                            staticData.stop.id,
                            alerts,
                            hasSchedulesTodayByPattern
                        )
                    is NearbyStaticData.StaticPatterns.ByDirection ->
                        RealtimePatterns.ByDirection(
                            it,
                            upcomingTripsMap,
                            staticData.stop.id,
                            alerts,
                            hasSchedulesTodayByPattern
                        )
                }
            }
            .filter {
                (it.isTypical() || it.isUpcomingWithin(filterTime, cutoffTime)) &&
                    !it.isArrivalOnly()
            }
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
}
