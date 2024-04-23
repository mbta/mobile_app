package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse

data class Direction(
    var name: String,
    var destination: String,
) {
    constructor(
        directionId: Int,
        stop: Stop,
        route: Route,
        routeStopIds: List<String>?
    ) : this(
        name = route.directionNames[directionId] ?: "",
        destination = getSpecialCaseDestination(stop.id, route.id, routeStopIds, directionId)
                ?: route.directionDestinations[directionId] ?: ""
    )

    companion object {
        private val specialCases: Map<String, List<List<Pair<String, String?>>?>> =
            mapOf(
                Pair(
                    "Green",
                    listOf(
                        listOf(
                            Pair("place-armnl", "Copley & West"),
                            Pair("place-hymnl", "Kenmore & West"),
                            Pair("place-prmnl", null),
                            Pair("place-kencl", null)
                        ),
                        listOf(
                            Pair("place-pktrm", "Gov Ctr & North"),
                            Pair("place-haecl", "North Station & North"),
                            Pair("place-spmnl", "Lechmere & North"),
                            Pair("place-lech", null)
                        )
                    )
                ),
                Pair(
                    "Red",
                    listOf(
                        listOf(
                            Pair("place-jfk", null),
                            Pair("place-asmnl", "Ashmont"),
                            Pair("place-brntn", "Braintree")
                        ),
                        null
                    )
                )
            )

        private val idOverrides: Map<String, String> =
            mapOf(
                Pair("Green-B", "Green"),
                Pair("Green-C", "Green"),
                Pair("Green-D", "Green"),
                Pair("Green-E", "Green"),
            )

        fun getSpecialCaseDestination(
            stopId: String,
            routeId: String,
            possibleStops: List<String>?,
            directionId: Int
        ): String? {
            val routeCases = specialCases[idOverrides[routeId] ?: routeId] ?: return null
            val directionCases = routeCases[directionId] ?: return null
            val stops = possibleStops ?: return null
            val stopIndex = stops.indexOf(stopId)
            if (stopIndex == -1) {
                return null
            }
            return directionCases
                .firstOrNull { (caseStopId, _) -> stopIndex <= stops.indexOf(caseStopId) }
                ?.second
        }

        fun getDirections(
            global: GlobalResponse,
            stop: Stop,
            route: Route,
            patterns: List<RoutePattern>
        ): List<Direction> {
            val representativePatternsByDirection =
                patterns
                    .groupBy { pattern -> pattern.directionId }
                    .mapValues { directionPatterns ->
                        directionPatterns.value.minBy { pattern ->
                            pattern.typicality?.ordinal?.toDouble() ?: Double.MAX_VALUE
                        }
                    }

            val tripParentIds =
                representativePatternsByDirection.mapValues { pattern ->
                    global.trips[pattern.value.representativeTripId]?.stopIds?.map { stopId ->
                        global.stops[stopId]?.parentStationId ?: stopId
                    }
                        ?: listOf()
                }

            return listOf(0, 1).map { directionId ->
                Direction(directionId, stop, route, tripParentIds[directionId])
            }
        }
    }
}
