package com.mbta.tid.mbta_app

import kotlin.test.Test
import kotlin.test.assertEquals

class NearbyResponseTest {
    @Test
    fun testRoutePatternsByStop() {
        val stop1 = Stop("1", 1.2, 3.4, "A", null)
        val stop2 = Stop("2", 5.6, 7.8, "B", null)
        val stop3 = Stop("3", 9.1, 2.3, "C", null)

        val route1 = Route("1", "", emptyList(), emptyList(), "Route One", "1", 1, "")
        val route2 = Route("2", "", emptyList(), emptyList(), "Route Two", "2", 2, "")

        val rp10 = RoutePattern("1-0", 0, "1 Outbound", 10, route1)
        val rp11 = RoutePattern("1-1", 1, "1 Inbound", 11, route1)
        val rp20 = RoutePattern("2-0", 0, "2 Eastbound", 20, route2)
        val rp21 = RoutePattern("2-1", 1, "2 Westbound", 21, route2)

        val response =
            NearbyResponse(
                stops = listOf(stop1, stop2, stop3),
                routePatterns = listOf(rp10, rp11, rp20, rp21).associateBy { it.id },
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(rp10.id),
                        stop2.id to listOf(rp10.id, rp11.id, rp21.id),
                        stop3.id to listOf(rp10.id, rp20.id)
                    )
            )

        assertEquals(
            response.routePatternsByStop(),
            listOf(rp10 to stop1, rp11 to stop2, rp21 to stop2, rp20 to stop3)
        )
    }
}
