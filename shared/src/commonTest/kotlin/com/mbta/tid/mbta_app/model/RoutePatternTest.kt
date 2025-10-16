package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.RoutePattern.PatternsForStop
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.test.Test
import kotlin.test.assertEquals

internal class RoutePatternTest {

    val objects = ObjectCollectionBuilder()
    val stop1 = objects.stop {}
    val stop2 = objects.stop {}
    val stop3 = objects.stop {}

    val route = objects.route {}
    val rp1 = objects.routePattern(route) { directionId = 0 }
    val rp2 = objects.routePattern(route) { directionId = 1 }

    val global =
        GlobalResponse(
            objects,
            patternIdsByStop =
                mapOf(
                    stop1.id to listOf(rp1.id),
                    stop2.id to listOf(rp1.id),
                    stop3.id to listOf(rp1.id, rp2.id),
                ),
        )

    @Test
    fun `test patternsGroupedByLineOrRouteAndStop includes only closest stops with unique service`() {
        val expected: Map<LineOrRoute, Map<Stop, PatternsForStop>> =
            mapOf(
                LineOrRoute.Route(route) to
                    mapOf(
                        stop1 to
                            PatternsForStop(
                                allPatterns = listOf(rp1),
                                patternsNotSeenAtEarlierStops = setOf(rp1.id),
                            ),
                        stop3 to
                            PatternsForStop(
                                allPatterns = listOf(rp1, rp2),
                                patternsNotSeenAtEarlierStops = setOf(rp2.id),
                            ),
                    )
            )

        assertEquals(
            expected,
            RoutePattern.patternsGroupedByLineOrRouteAndStop(
                Stop.resolvedParentToAllStops(listOf(stop1.id, stop2.id, stop3.id), global),
                global,
                RouteCardData.Context.NearbyTransit,
            ),
        )
    }

    @Test
    fun `test patternsGroupedByLineOrRouteAndStop includes redundant stops for favorites`() {
        val expected: Map<LineOrRoute, Map<Stop, PatternsForStop>> =
            mapOf(
                LineOrRoute.Route(route) to
                    mapOf(
                        stop1 to
                            PatternsForStop(
                                allPatterns = listOf(rp1),
                                patternsNotSeenAtEarlierStops = setOf(rp1.id),
                            ),
                        stop2 to
                            PatternsForStop(
                                allPatterns = listOf(rp1),
                                patternsNotSeenAtEarlierStops = setOf(rp1.id),
                            ),
                        stop3 to
                            PatternsForStop(
                                allPatterns = listOf(rp1, rp2),
                                patternsNotSeenAtEarlierStops = setOf(rp1.id, rp2.id),
                            ),
                    )
            )
        assertEquals(
            expected,
            RoutePattern.patternsGroupedByLineOrRouteAndStop(
                Stop.resolvedParentToAllStops(listOf(stop1.id, stop2.id, stop3.id), global),
                global,
                RouteCardData.Context.Favorites,
            ),
        )
    }

    @Test
    fun `test patternsGroupedByLineOrRouteAndStop filters non-favorite stops for favorites`() {
        val expected: Map<LineOrRoute, Map<Stop, PatternsForStop>> =
            mapOf(
                LineOrRoute.Route(route) to
                    mapOf(
                        stop1 to
                            PatternsForStop(
                                allPatterns = listOf(rp1),
                                patternsNotSeenAtEarlierStops = setOf(rp1.id),
                            ),
                        stop3 to
                            PatternsForStop(
                                allPatterns = listOf(rp2),
                                patternsNotSeenAtEarlierStops = setOf(rp1.id, rp2.id),
                            ),
                    )
            )
        assertEquals(
            expected,
            RoutePattern.patternsGroupedByLineOrRouteAndStop(
                Stop.resolvedParentToAllStops(listOf(stop1.id, stop2.id, stop3.id), global),
                global,
                RouteCardData.Context.Favorites,
                setOf(
                    RouteStopDirection(route.id, stop1.id, rp1.directionId),
                    RouteStopDirection(route.id, stop3.id, rp2.directionId),
                ),
            ),
        )
    }
}
