package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse

data class Direction(
    var name: String,
    var destination: String?,
    var id: Int,
) {
    constructor(
        directionId: Int,
        route: Route,
        stop: Stop? = null,
        routeStopIds: List<String>? = null
    ) : this(
        name = route.directionNames[directionId] ?: "",
        destination = getSpecialCaseDestination(directionId, route.id, stop?.id, routeStopIds)
                ?: route.directionDestinations[directionId] ?: "",
        directionId
    )

    companion object {
        // This is split into a separate variable for a hardcoded check
        private val northStationDestination = "North Station & North"
        /*
        This is a map containing all the special case direction labels for branching routes.

        The top level key is the route ID (or route alias defined in idOverrides).
        The value of the top level map is a list containing two other lists, with the index
        corresponding to each of the direction IDs for that route. If one of these lists is null,
        there are no special cases for that route and direction (like northbound Red line).

        The list for each direction contains a sequence of pairs of stop IDs and override labels.
        The stop IDs are ordered as they appear along the typical route pattern for that direction.
        Only parent stop IDs can be used, and entries are added for every label change that happens
        as you move along the route.

        The `getSpecialCaseDestination` function will find where the provided stop ID is in relation
        to the (id, label) pairs, and returns the first label with an ID that exists in the
        `routeStopIds` sequence after (or equal to) the provided stop ID.

        A null value means to ignore special cases and fall back to the route's default labels.
         */
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
                            Pair("place-boyls", "Park St & North"),
                            Pair("place-pktrm", "Gov Ctr & North"),
                            Pair("place-haecl", northStationDestination),
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
            directionId: Int,
            routeId: String,
            stopId: String?,
            routeStopIds: List<String>?,
        ): String? {
            val routeCases = specialCases[idOverrides[routeId] ?: routeId] ?: return null
            val directionCases = routeCases[directionId] ?: return null
            val stops = routeStopIds ?: return null
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
            if (!specialCases.containsKey(idOverrides[route.id] ?: route.id)) {
                return listOf(0, 1).map { directionId -> Direction(directionId, route) }
            }

            val stopSequenceByDirection = getTypicalStopSequenceByDirection(patterns, global)
            return listOf(0, 1).map { directionId ->
                Direction(directionId, route, stop, stopSequenceByDirection[directionId])
            }
        }

        fun getDirectionsForLine(
            global: GlobalResponse,
            stop: Stop,
            patterns: List<RoutePattern>
        ): List<Direction> {
            if (patterns.isEmpty()) {
                // If no patterns were provided, something is wrong, return dummy directions
                return listOf(0, 1).map { Direction("", "", it) }
            }

            val directionsByRoute =
                patterns
                    .groupBy { it.routeId }
                    .mapValues {
                        val route = global.routes[it.key] ?: return emptyList()
                        getDirections(global, stop, route, it.value)
                    }

            return listOf(0, 1).map { directionId ->
                val directionsByDestination =
                    directionsByRoute
                        .mapNotNull { it.value.getOrNull(directionId) }
                        .associateBy { it.destination }

                if (directionsByDestination.size == 1) {
                    // When only one direction is in the set, it means that all the routes in the
                    // line share the same destination at this stop, so we can safely display it.
                    directionsByDestination.values.first()
                } else if (directionsByDestination.isNotEmpty()) {
                    // Handle the unique mid-route terminal case at Government Center
                    val specialCase = govCenterSpecialCase(directionsByDestination)
                    if (specialCase != null) {
                        return@map specialCase
                    }
                    // When multiple destinations are served in one direction, we don't want to
                    // display any destination label, so it's set to null.
                    val representativeDirection = directionsByDestination.values.first()
                    Direction(representativeDirection.name, null, directionId)
                } else {
                    // If this is true, the direction isn't served and shouldn't be displayed,
                    // or something is wrong with the provided data.
                    Direction("", "", directionId)
                }
            }
        }

        // This is hacky, but seemed like the best way to handle this case, where the Green Line
        // has multiple routes which terminate mid-line at Gov Center, but since those routes are
        // served at the stop, it has non-null Direction objects with Gov Center destinations. This
        // checks if we have this specific case, and returns the North direction if we do.
        private fun govCenterSpecialCase(
            directionsByDestination: Map<String?, Direction>
        ): Direction? {
            val govCenterDestinations = setOf("Government Center", northStationDestination)
            if (directionsByDestination.keys == govCenterDestinations) {
                return directionsByDestination[northStationDestination]
            }
            return null
        }

        private fun getTypicalStopSequenceByDirection(
            patterns: List<RoutePattern>,
            global: GlobalResponse
        ): Map<Int, List<String>?> {
            return patterns
                .groupBy { pattern -> pattern.directionId }
                .mapValues { directionPatterns ->
                    val typicalTripId =
                        directionPatterns.value
                            .firstOrNull { pattern ->
                                pattern.typicality == RoutePattern.Typicality.Typical
                            }
                            ?.representativeTripId
                    global.trips[typicalTripId]?.stopIds?.map { stopId ->
                        global.stops[stopId]?.parentStationId ?: stopId
                    }
                }
        }
    }
}
