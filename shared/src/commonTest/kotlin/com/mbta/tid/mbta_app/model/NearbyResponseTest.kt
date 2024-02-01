package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.NearbyResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class NearbyResponseTest {

    @Test
    fun `byRouteAndStop when a route pattern serves multiple stops it is only included for the first one`() {

        val stop1 = Stop("1", 1.2, 3.4, "A", null)
        val stop2 = Stop("2", 5.6, 7.8, "B", null)

        val route1 = Route("1", "", emptyList(), emptyList(), "Route One", "1", 1, "")

        val route1rp1 = RoutePattern("1-0", 0, "1 Outbound", 10, route1)
        val route1rp2 = RoutePattern("1-1", 1, "1 Inbound", 11, route1)

        val response =
            NearbyResponse(
                stops = listOf(stop1, stop2),
                routePatterns = listOf(route1rp1, route1rp2).associateBy { it.id },
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(route1rp1.id),
                        stop2.id to listOf(route1rp1.id, route1rp2.id),
                    )
            )

        assertEquals(
            listOf(
                NearbyRoute(
                    route1,
                    listOf(
                        NearbyPatternsByStop(stop1, listOf(route1rp1)),
                        NearbyPatternsByStop(stop2, listOf(route1rp2))
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

        val route1rp1 = RoutePattern("1-0", 0, "1 Outbound", 10, route1)

        val response =
            NearbyResponse(
                stops = listOf(stop1, stop2),
                routePatterns = listOf(route1rp1).associateBy { it.id },
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(route1rp1.id),
                        stop2.id to listOf(route1rp1.id),
                    )
            )

        assertEquals(
            listOf(
                NearbyRoute(route1, listOf(NearbyPatternsByStop(stop1, listOf(route1rp1)))),
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

        val route1rp1 = RoutePattern("1-0", 0, "1 Outbound", 10, route1)
        val route1rp2 = RoutePattern("1-1", 1, "1 Inbound", 11, route1)
        val route1rp3 = RoutePattern("1-2", 1, "1 Inbound via Back By", 12, route1)

        val route2rp1 = RoutePattern("2-0", 0, "2 Eastbound", 20, route2)

        val response =
            NearbyResponse(
                stops = listOf(stop1, stop2),
                routePatterns =
                    listOf(route1rp1, route1rp2, route1rp3, route2rp1).associateBy { it.id },
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(route1rp1.id, route1rp2.id),
                        stop2.id to listOf(route1rp1.id, route1rp3.id, route2rp1.id),
                    )
            )

        assertEquals(
            listOf(
                NearbyRoute(
                    route1,
                    listOf(
                        NearbyPatternsByStop(stop1, listOf(route1rp1, route1rp2)),
                        NearbyPatternsByStop(stop2, listOf(route1rp3))
                    )
                ),
                NearbyRoute(route2, listOf(NearbyPatternsByStop(stop2, listOf(route2rp1))))
            ),
            response.byRouteAndStop()
        )
    }
}
