package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.StopAndRoutePatternResponse

/** Aggregates stops and the patterns that serve them by route. */
data class NearbyStaticData(val data: List<RouteWithStops>) {
    data class HeadsignWithPatterns(val headsign: String, val patterns: List<RoutePattern>) :
        Comparable<HeadsignWithPatterns> {
        override fun compareTo(other: HeadsignWithPatterns): Int =
            patterns.first().compareTo(other.patterns.first())
    }

    data class StopWithPatterns(
        val stop: Stop,
        /** Includes both parent and child stop IDs if present */
        val allStopIds: Set<String>,
        val patternsByHeadsign: List<HeadsignWithPatterns>
    )

    data class RouteWithStops(val route: Route, val patternsByStop: List<StopWithPatterns>)

    fun stopIds(): Set<String> =
        data.flatMapTo(mutableSetOf()) { (_, patternsByStop) -> patternsByStop.map { it.stop.id } }

    constructor(
        response: StopAndRoutePatternResponse
    ) : this(
        response.run {
            val routePatternsUsed = mutableSetOf<String>()

            val patternsByRouteAndStop =
                mutableMapOf<Route, MutableMap<Stop, MutableList<RoutePattern>>>()

            val fullStopIds = mutableMapOf<String, MutableSet<String>>()

            response.stops.forEach { stop ->
                val newPatternIds =
                    response.patternIdsByStop
                        .getOrElse(stop.id) { emptyList() }
                        .filter { !routePatternsUsed.contains(it) }
                routePatternsUsed.addAll(newPatternIds)

                val newPatternsByRoute =
                    newPatternIds
                        .map { patternId -> response.routePatterns.getValue(patternId) }
                        .groupBy { it.routeId }

                if (newPatternsByRoute.isEmpty()) {
                    return@forEach
                }

                val stopKey =
                    if (stop.parentStation != null) {
                        fullStopIds
                            .getOrPut(stop.parentStation.id) { mutableSetOf(stop.parentStation.id) }
                            .add(stop.id)
                        stop.parentStation
                    } else {
                        stop
                    }

                newPatternsByRoute.forEach { (routeId, routePatterns) ->
                    val routeStops =
                        patternsByRouteAndStop.getOrPut(response.routes.getValue(routeId)) {
                            mutableMapOf()
                        }
                    val patternsForStop = routeStops.getOrPut(stopKey) { mutableListOf() }
                    patternsForStop += routePatterns
                }
            }

            patternsByRouteAndStop.map { (route, patternsByStop) ->
                RouteWithStops(
                    route = route,
                    patternsByStop =
                        patternsByStop.map { (stop, patterns) ->
                            StopWithPatterns(
                                stop = stop,
                                allStopIds = fullStopIds.getOrElse(stop.id) { setOf(stop.id) },
                                patternsByHeadsign =
                                    patterns
                                        .groupBy { it.representativeTrip!!.headsign }
                                        .map { (headsign, routePatterns) ->
                                            HeadsignWithPatterns(headsign, routePatterns.sorted())
                                        }
                                        .sorted()
                            )
                        }
                )
            }
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
        val builder = PatternsByStopBuilder()
        builder.block()
        data.add(NearbyStaticData.RouteWithStops(route, builder.data))
    }

    class PatternsByHeadsignBuilder {
        val data = mutableListOf<NearbyStaticData.HeadsignWithPatterns>()

        fun headsign(headsign: String, patterns: List<RoutePattern>) {
            data.add(NearbyStaticData.HeadsignWithPatterns(headsign, patterns))
        }
    }

    class PatternsByStopBuilder {
        val data = mutableListOf<NearbyStaticData.StopWithPatterns>()

        @DefaultArgumentInterop.Enabled
        fun stop(
            stop: Stop,
            childStopIds: List<String> = emptyList(),
            block: PatternsByHeadsignBuilder.() -> Unit
        ) {
            val builder = PatternsByHeadsignBuilder()
            builder.block()
            data.add(
                NearbyStaticData.StopWithPatterns(
                    stop,
                    setOf(stop.id).plus(childStopIds),
                    builder.data
                )
            )
        }
    }
}
