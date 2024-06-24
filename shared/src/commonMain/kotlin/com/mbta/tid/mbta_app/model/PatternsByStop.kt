package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.RealtimePatterns
import com.mbta.tid.mbta_app.model.response.UpcomingTripsMap
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
        cutoffTime: Instant,
        alerts: Collection<Alert>?,
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
                            staticData.allStopIds,
                            alerts
                        )
                    is NearbyStaticData.StaticPatterns.ByDirection ->
                        RealtimePatterns.ByDirection(
                            it,
                            upcomingTripsMap,
                            staticData.allStopIds,
                            alerts
                        )
                }
            }
            .filter { (it.isTypical() || it.isUpcomingBefore(cutoffTime)) && !it.isArrivalOnly() }
            .sorted(),
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
}
