package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.StopAndRoutePatternResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class NearbyResponseTest {

    @Test
    fun `byRouteAndStop when a route pattern serves multiple stops it is only included for the first one`() {

        val stop1 = Stop("1", 1.2, 3.4, "A", null)
        val stop2 = Stop("2", 5.6, 7.8, "B", null)

        val route1 =
            Route("1", RouteType.BUS, "", emptyList(), emptyList(), "Route One", "1", 1, "")

        val route1rp1 =
            RoutePattern("1-0", 0, "1 Outbound", 10, Trip("trip1", "Harvard"), route1.id)
        val route1rp2 = RoutePattern("1-1", 1, "1 Inbound", 11, Trip("trip2", "Nubian"), route1.id)

        val response =
            StopAndRoutePatternResponse(
                stops = listOf(stop1, stop2),
                routePatterns = listOf(route1rp1, route1rp2).associateBy { it.id },
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(route1rp1.id),
                        stop2.id to listOf(route1rp1.id, route1rp2.id),
                    ),
                routes = mapOf("1" to route1)
            )

        assertEquals(
            listOf(
                StopAssociatedRoute(
                    route1,
                    listOf(
                        PatternsByStop(
                            stop1,
                            listOf(PatternsByHeadsign("Harvard", listOf(route1rp1)))
                        ),
                        PatternsByStop(
                            stop2,
                            listOf(PatternsByHeadsign("Nubian", listOf(route1rp2)))
                        )
                    )
                ),
            ),
            response.byRouteAndStop()
        )
    }

    @Test
    fun `byRouteAndStop route patterns are sorted by their sort order`() {

        val stop1 = Stop("1", 1.2, 3.4, "A", null)

        val route1 =
            Route("1", RouteType.BUS, "", emptyList(), emptyList(), "Route One", "1", 1, "")

        val route1rp1 = RoutePattern("1-0", 0, "1 Outbound", 1, Trip("trip1", "Harvard"), route1.id)
        val route1rp2 = RoutePattern("1-1", 1, "1 Inbound", 2, Trip("trip2", "Nubian"), route1.id)

        val response =
            StopAndRoutePatternResponse(
                stops = listOf(stop1),
                routePatterns = listOf(route1rp2, route1rp1).associateBy { it.id },
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(route1rp2.id, route1rp1.id),
                    ),
                routes = mapOf("1" to route1)
            )

        assertEquals(
            listOf(
                StopAssociatedRoute(
                    route1,
                    listOf(
                        PatternsByStop(
                            stop1,
                            listOf(
                                PatternsByHeadsign("Harvard", listOf(route1rp1)),
                                PatternsByHeadsign("Nubian", listOf(route1rp2))
                            )
                        ),
                    )
                ),
            ),
            response.byRouteAndStop()
        )
    }

    @Test
    fun `byRouteAndStop groups patterns by headsign`() {

        val stop1 = Stop("1", 1.2, 3.4, "A", null)

        val route1 =
            Route("1", RouteType.BUS, "", emptyList(), emptyList(), "Route One", "1", 1, "")

        val route1rp1 = RoutePattern("1-0", 0, "1 Outbound", 1, Trip("trip1", "Harvard"), route1.id)
        val route1rp2 =
            RoutePattern("1-1", 0, "1 Outbound V2", 1, Trip("trip2", "Harvard"), route1.id)

        val route1rp3 = RoutePattern("1-0-1", 1, "1 Inbound", 2, Trip("trip3", "Nubian"), route1.id)

        val response =
            StopAndRoutePatternResponse(
                stops = listOf(stop1),
                routePatterns = listOf(route1rp1, route1rp2, route1rp3).associateBy { it.id },
                routes = mapOf(route1.id to route1),
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(route1rp1.id, route1rp2.id, route1rp3.id),
                    )
            )

        assertEquals(
            listOf(
                StopAssociatedRoute(
                    route1,
                    listOf(
                        PatternsByStop(
                            stop1,
                            listOf(
                                PatternsByHeadsign("Harvard", listOf(route1rp1, route1rp2)),
                                PatternsByHeadsign("Nubian", listOf(route1rp3))
                            )
                        )
                    )
                ),
            ),
            response.byRouteAndStop()
        )
    }

    @Test
    fun `byRouteAndStop when there are no new route patterns for a stop then it is omitted`() {

        val stop1 = Stop("1", 1.2, 3.4, "A", null)
        val stop2 = Stop("2", 5.6, 7.8, "B", null)

        val route1 =
            Route("1", RouteType.BUS, "", emptyList(), emptyList(), "Route One", "1", 1, "")

        val route1rp1 =
            RoutePattern("1-0", 0, "1 Outbound", 10, Trip("trip1", "Harvard"), route1.id)

        val response =
            StopAndRoutePatternResponse(
                stops = listOf(stop1, stop2),
                routePatterns = listOf(route1rp1).associateBy { it.id },
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(route1rp1.id),
                        stop2.id to listOf(route1rp1.id),
                    ),
                routes = mapOf("1" to route1)
            )

        assertEquals(
            listOf(
                StopAssociatedRoute(
                    route1,
                    listOf(
                        PatternsByStop(
                            stop1,
                            listOf(PatternsByHeadsign("Harvard", listOf(route1rp1)))
                        )
                    )
                ),
            ),
            response.byRouteAndStop()
        )
    }

    @Test
    fun `byRouteAndStop when a stop is served by multiple routes it is included for each route`() {

        val stop1 = Stop("1", 1.2, 3.4, "A", null)
        val stop2 = Stop("2", 5.6, 7.8, "B", null)

        val route1 =
            Route("1", RouteType.BUS, "", emptyList(), emptyList(), "Route One", "1", 1, "")
        val route2 =
            Route("2", RouteType.BUS, "", emptyList(), emptyList(), "Route Two", "2", 2, "")

        val route1rp1 =
            RoutePattern("1-0", 0, "1 Outbound", 10, Trip("trip1", "Harvard"), route1.id)
        val route1rp2 = RoutePattern("1-1", 1, "1 Inbound", 11, Trip("trip2", "Nubian"), route1.id)
        val route1rp3 =
            RoutePattern(
                "1-2",
                1,
                "1 Inbound via Back By",
                12,
                Trip("trip3", "Nubian via Allston"),
                route1.id
            )

        val route2rp1 =
            RoutePattern("2-0", 0, "2 Eastbound", 20, Trip("trip4", "Porter Sq"), route2.id)

        val response =
            StopAndRoutePatternResponse(
                stops = listOf(stop1, stop2),
                routePatterns =
                    listOf(route1rp1, route1rp2, route1rp3, route2rp1).associateBy { it.id },
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(route1rp1.id, route1rp2.id),
                        stop2.id to listOf(route1rp1.id, route1rp3.id, route2rp1.id),
                    ),
                routes = mapOf("1" to route1, "2" to route2)
            )

        assertEquals(
            listOf(
                StopAssociatedRoute(
                    route1,
                    listOf(
                        PatternsByStop(
                            stop1,
                            listOf(
                                PatternsByHeadsign("Harvard", listOf(route1rp1)),
                                PatternsByHeadsign("Nubian", listOf(route1rp2))
                            )
                        ),
                        PatternsByStop(
                            stop2,
                            listOf(PatternsByHeadsign("Nubian via Allston", listOf(route1rp3)))
                        )
                    )
                ),
                StopAssociatedRoute(
                    route2,
                    listOf(
                        PatternsByStop(
                            stop2,
                            listOf(PatternsByHeadsign("Porter Sq", listOf(route2rp1)))
                        )
                    )
                )
            ),
            response.byRouteAndStop()
        )
    }

    @Test
    fun `byRouteAndStop groups by parent station`() {

        val station1 = Stop("1", 1.2, 3.4, "A", null)

        val station1stop1 = Stop("1-1", 1.2, 3.4, "A", station1)
        val station1stop2 = Stop("1-2", 1.2, 3.4, "A", station1)

        val stop2 = Stop("2", 5.6, 7.8, "B", null)

        val route1 =
            Route("1", RouteType.BUS, "", emptyList(), emptyList(), "Route One", "1", 1, "")

        val route1rp1 =
            RoutePattern("1-0", 0, "1 Outbound", 10, Trip("trip1", "Harvard"), route1.id)
        val route1rp2 = RoutePattern("1-1", 1, "1 Inbound", 11, Trip("trip2", "Nubian"), route1.id)
        val route1rp3 =
            RoutePattern(
                "1-2",
                1,
                "1 Inbound via Back By",
                12,
                Trip("trip3", "Nubian via Allston"),
                route1.id
            )

        val response =
            StopAndRoutePatternResponse(
                stops = listOf(station1stop1, station1stop2, stop2),
                routePatterns = listOf(route1rp1, route1rp2, route1rp3).associateBy { it.id },
                patternIdsByStop =
                    mapOf(
                        station1stop1.id to
                            listOf(
                                route1rp1.id,
                            ),
                        station1stop2.id to
                            listOf(
                                route1rp2.id,
                            ),
                        stop2.id to
                            listOf(
                                route1rp3.id,
                            ),
                    ),
                routes = mapOf("1" to route1)
            )

        assertEquals(
            listOf(
                StopAssociatedRoute(
                    route1,
                    listOf(
                        PatternsByStop(
                            station1,
                            listOf(
                                PatternsByHeadsign("Harvard", listOf(route1rp1)),
                                PatternsByHeadsign("Nubian", listOf(route1rp2))
                            )
                        ),
                        PatternsByStop(
                            stop2,
                            listOf(PatternsByHeadsign("Nubian via Allston", listOf(route1rp3)))
                        )
                    )
                ),
            ),
            response.byRouteAndStop()
        )
    }
}
