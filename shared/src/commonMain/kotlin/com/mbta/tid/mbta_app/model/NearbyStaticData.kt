package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse

/**
 * Aggregates stops and the patterns that serve them by route. The list of routes is ordered with
 * subway routes first, then sorted by distance. Ties are broken by the sort order of the first
 * route pattern.
 */
data class NearbyStaticData(val data: List<RouteWithStops>) {
    data class HeadsignWithPatterns(
        val route: Route,
        val headsign: String,
        val patterns: List<RoutePattern>
    ) : Comparable<HeadsignWithPatterns> {
        override fun compareTo(other: HeadsignWithPatterns): Int =
            patterns.first().compareTo(other.patterns.first())
    }

    data class StopWithPatterns(
        val route: Route,
        val stop: Stop,
        /** Includes both parent and child stop IDs if present */
        val allStopIds: Set<String>,
        val patternsByHeadsign: List<HeadsignWithPatterns>,
        val directions: List<Direction>
    )

    data class RouteWithStops(val route: Route, val patternsByStop: List<StopWithPatterns>)

    fun stopIds(): Set<String> =
        data.flatMapTo(mutableSetOf()) { (_, patternsByStop) -> patternsByStop.map { it.stop.id } }

    constructor(
        global: GlobalResponse,
        nearby: NearbyResponse
    ) : this(
        global.run {
            val routePatternsUsed = mutableSetOf<String>()

            val patternsByRouteAndStop =
                mutableMapOf<Route, MutableMap<Stop, MutableList<RoutePattern>>>()

            val fullStopIds = mutableMapOf<String, MutableSet<String>>()

            nearby.stopIds.forEach { stopId ->
                val stop = stops.getValue(stopId)
                val newPatternIds =
                    patternIdsByStop
                        .getOrElse(stop.id) { emptyList() }
                        .filter { !routePatternsUsed.contains(it) }
                routePatternsUsed.addAll(newPatternIds)

                val newPatternsByRoute =
                    newPatternIds
                        .map { patternId -> routePatterns.getValue(patternId) }
                        .groupBy { it.routeId }

                val stopKey =
                    stop.parentStationId?.let { parentStationId ->
                        fullStopIds
                            .getOrPut(parentStationId) { mutableSetOf(parentStationId) }
                            .add(stop.id)
                        // Parents should be disjoint, but if somehow a parent has its own patterns,
                        // find it in the regular stops list
                        stops.getValue(parentStationId)
                    }
                        ?: stop

                newPatternsByRoute.forEach { (routeId, routePatterns) ->
                    val routeStops =
                        patternsByRouteAndStop.getOrPut(routes.getValue(routeId)) { mutableMapOf() }
                    val patternsForStop = routeStops.getOrPut(stopKey) { mutableListOf() }
                    patternsForStop += routePatterns
                }
            }

            patternsByRouteAndStop
                .map { (route, patternsByStop) ->
                    RouteWithStops(
                        route = route,
                        patternsByStop =
                            patternsByStop.map { (stop, patterns) ->
                                StopWithPatterns(
                                    route = route,
                                    stop = stop,
                                    allStopIds = fullStopIds.getOrElse(stop.id) { setOf(stop.id) },
                                    patternsByHeadsign =
                                        patterns
                                            .groupBy {
                                                val representativeTrip =
                                                    trips.getValue(it.representativeTripId)
                                                representativeTrip.headsign
                                            }
                                            .map { (headsign, routePatterns) ->
                                                HeadsignWithPatterns(
                                                    route,
                                                    headsign,
                                                    routePatterns.sorted()
                                                )
                                            }
                                            .sorted(),
                                    directions =
                                        Direction.getDirections(global, stop, route, patterns)
                                )
                            }
                    )
                }
                .sortedWith(compareBy(Route.subwayFirstComparator) { it.route })
        }
    )

    companion object {
        fun build(block: NearbyStaticDataBuilder.() -> Unit): NearbyStaticData {
            val builder = NearbyStaticDataBuilder()
            builder.block()
            return NearbyStaticData(builder.data)
        }
    }
}

class NearbyStaticDataBuilder {
    val data = mutableListOf<NearbyStaticData.RouteWithStops>()

    fun route(route: Route, block: PatternsByStopBuilder.() -> Unit) {
        val builder = PatternsByStopBuilder(route)
        builder.block()
        data.add(NearbyStaticData.RouteWithStops(route, builder.data))
    }

    class PatternsByHeadsignBuilder(val route: Route) {
        val data = mutableListOf<NearbyStaticData.HeadsignWithPatterns>()

        fun headsign(headsign: String, patterns: List<RoutePattern>) {
            data.add(NearbyStaticData.HeadsignWithPatterns(route, headsign, patterns))
        }
    }

    class PatternsByStopBuilder(val route: Route) {
        val data = mutableListOf<NearbyStaticData.StopWithPatterns>()

        @DefaultArgumentInterop.Enabled
        fun stop(
            stop: Stop,
            childStopIds: List<String> = emptyList(),
            block: PatternsByHeadsignBuilder.() -> Unit
        ) {
            val builder = PatternsByHeadsignBuilder(route)
            builder.block()
            data.add(
                NearbyStaticData.StopWithPatterns(
                    route,
                    stop,
                    setOf(stop.id).plus(childStopIds),
                    builder.data,
                    listOf(
                        Direction(name = "", destination = ""),
                        Direction(name = "", destination = "")
                    )
                )
            )
        }
    }
}
