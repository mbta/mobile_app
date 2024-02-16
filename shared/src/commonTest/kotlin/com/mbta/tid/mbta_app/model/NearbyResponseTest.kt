package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.StopAndRoutePatternResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class NearbyResponseTest {

    @Test
    fun `byRouteAndStop when a route pattern serves multiple stops it is only included for the first one`() {

        val stop1 = Stop("1", 1.2, 3.4, "A", null)
        val stop2 = Stop("2", 5.6, 7.8, "B", null)

        val route1 = Route("1", "", emptyList(), emptyList(), "Route One", "1", 1, "")

        val route1rp1 = RoutePattern("1-0", 0, "1 Outbound", 10, "1")
        val route1rp2 = RoutePattern("1-1", 1, "1 Inbound", 11, "1")

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
                        PatternsByStop(stop1, listOf(route1rp1)),
                        PatternsByStop(stop2, listOf(route1rp2))
                    )
                ),
            ),
            response.byRouteAndStop()
        )
    }

    @Test
    fun `byRouteAndStop route patterns are sorted by their sort order`() {

        val stop1 = Stop("1", 1.2, 3.4, "A", null)

        val route1 = Route("1", "", emptyList(), emptyList(), "Route One", "1", 1, "")

        val route1rp1 = RoutePattern("1-0", 0, "1 Outbound", 1, "1")
        val route1rp2 = RoutePattern("1-1", 1, "1 Inbound", 2, "1")

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
                        PatternsByStop(stop1, listOf(route1rp1, route1rp2)),
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

        val route1 = Route("1", "", emptyList(), emptyList(), "Route One", "1", 1, "")

        val route1rp1 = RoutePattern("1-0", 0, "1 Outbound", 10, "1")

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
                StopAssociatedRoute(route1, listOf(PatternsByStop(stop1, listOf(route1rp1)))),
            ),
            response.byRouteAndStop()
        )
    }

    @Test
    fun `byRouteAndStop when there is a stop that is served by multiple routes`() {

        val stop1 = Stop("1", 1.2, 3.4, "A", null)
        val stop2 = Stop("2", 5.6, 7.8, "B", null)

        val route1 = Route("1", "", emptyList(), emptyList(), "Route One", "1", 1, "")
        val route2 = Route("2", "", emptyList(), emptyList(), "Route Two", "2", 2, "")

        val route1rp1 = RoutePattern("1-0", 0, "1 Outbound", 10, "1")
        val route1rp2 = RoutePattern("1-1", 1, "1 Inbound", 11, "1")
        val route1rp3 = RoutePattern("1-2", 1, "1 Inbound via Back By", 12, "1")

        val route2rp1 = RoutePattern("2-0", 0, "2 Eastbound", 20, "2")

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
                        PatternsByStop(stop1, listOf(route1rp1, route1rp2)),
                        PatternsByStop(stop2, listOf(route1rp3))
                    )
                ),
                StopAssociatedRoute(route2, listOf(PatternsByStop(stop2, listOf(route2rp1))))
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

        val route1 = Route("1", "", emptyList(), emptyList(), "Route One", "1", 1, "")

        val route1rp1 = RoutePattern("1-0", 0, "1 Outbound", 10, "1")
        val route1rp2 = RoutePattern("1-1", 1, "1 Inbound", 11, "1")
        val route1rp3 = RoutePattern("1-2", 1, "1 Inbound via Back By", 12, "1")

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
                        PatternsByStop(station1, listOf(route1rp1, route1rp2)),
                        PatternsByStop(stop2, listOf(route1rp3))
                    )
                ),
            ),
            response.byRouteAndStop()
        )
    }
}
