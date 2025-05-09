package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.RouteCardData.LineOrRoute
import com.mbta.tid.mbta_app.model.RoutePattern.PatternsForStop
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutePatternTest {

    @Test
    fun `test patternsGroupedByLineOrRouteAndStop includes only closest stops with unique service`() {
        val objects = ObjectCollectionBuilder()
        val stop1 = objects.stop {}
        val stop2 = objects.stop {}
        val stop3 = objects.stop {}

        val route = objects.route {}
        val rp1 = objects.routePattern(route)
        val rp2 = objects.routePattern(route)

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(rp1.id),
                        // stop2 won't be returned b/c redundant service to stop1
                        stop2.id to listOf(rp1.id),
                        stop3.id to listOf(rp1.id, rp2.id)
                    )
            )

        val expected: Map<LineOrRoute, Map<Stop, PatternsForStop>> =
            mapOf(
                LineOrRoute.Route(route) to
                    mapOf(
                        stop1 to
                            PatternsForStop(
                                allPatterns = listOf(rp1),
                                patternsNotSeenAtEarlierStops = setOf(rp1.id)
                            ),
                        stop3 to
                            PatternsForStop(
                                allPatterns = listOf(rp1, rp2),
                                patternsNotSeenAtEarlierStops = setOf(rp2.id)
                            )
                    )
            )

        assertEquals(
            expected,
            RoutePattern.patternsGroupedByLineOrRouteAndStop(
                Stop.resolvedParentToAllStops(listOf(stop1.id, stop2.id, stop3.id), global),
                global
            )
        )
    }
}
