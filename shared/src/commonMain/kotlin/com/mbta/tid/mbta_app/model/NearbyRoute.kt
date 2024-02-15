package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.NearbyResponse

/** @property patterns [RoutePattern] listed in ascending order based on [RoutePattern.sortOrder] */
data class PatternsByHeadsign(val headsign: String, val patterns: List<RoutePattern>)

/**
 * @property patternsByHeadsign [RoutePattern]s serving the stop grouped by headsign. The headsigns
 *   are listed in ascending order based on [RoutePattern.sortOrder]
 */
data class NearbyPatternsByStop(val stop: Stop, val patternsByHeadsign: List<PatternsByHeadsign>)

/**
 * @property patternsByStop A list of route patterns grouped by first the nearby station or stop
 *   that they serve.
 */
data class NearbyRoute(
    val route: Route,
    val patternsByStop: List<NearbyPatternsByStop>,
)

/**
 * Aggregate nearby stops and the patterns that serve them by route. Preserves the sort order of the
 * stops received by the server in [NearbyResponse.stops]
 */
fun NearbyResponse.byRouteAndStop(): List<NearbyRoute> {
    val routePatternsUsed = mutableSetOf<String>()

    val patternsByRouteAndStop = mutableMapOf<Route, MutableMap<Stop, MutableList<RoutePattern>>>()

    stops.forEach { stop ->
        val newPatternIds =
            patternIdsByStop
                .getOrElse(stop.id) { emptyList() }
                .filter { !routePatternsUsed.contains(it) }
        routePatternsUsed.addAll(newPatternIds)

        val newPatternsByRoute =
            newPatternIds
                .mapNotNull { patternId -> routePatterns[patternId] }
                .sortedBy { it.sortOrder }
                .groupBy { it.route }

        newPatternsByRoute.forEach { (route, routePatterns) ->
            val stopKey = stop.parentStation ?: stop
            val routeStops = patternsByRouteAndStop.getOrPut(route) { mutableMapOf() }
            val patternsForStop = routeStops.getOrPut(stopKey) { mutableListOf() }
            patternsForStop += routePatterns
        }
    }

    return patternsByRouteAndStop.map { (route, patternsByStop) ->
        NearbyRoute(
            route = route,
            patternsByStop =
                patternsByStop.map { (stop, patterns) ->
                    NearbyPatternsByStop(
                        stop = stop,
                        patternsByHeadsign =
                            patterns
                                .groupBy { it.representativeTrip.headsign }
                                .map { PatternsByHeadsign(it.key, it.value) }
                    )
                }
        )
    }
}
