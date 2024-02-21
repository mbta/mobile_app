package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.TestData.pattern
import com.mbta.tid.mbta_app.TestData.prediction
import com.mbta.tid.mbta_app.TestData.route
import com.mbta.tid.mbta_app.TestData.routePattern
import com.mbta.tid.mbta_app.TestData.stop
import com.mbta.tid.mbta_app.TestData.trip
import com.mbta.tid.mbta_app.model.response.StopAndRoutePatternResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Instant

class NearbyResponseTest {

    @Test
    fun `byRouteAndStop when a route pattern serves multiple stops it is only included for the first one`() {

        val stop1 = stop()
        val stop2 = stop()

        val route1 = route()

        val route1rp1 = route1.pattern(representativeTrip = trip(headsign = "Harvard"))
        val route1rp2 = route1.pattern(representativeTrip = trip(headsign = "Nubian"))

        val response =
            StopAndRoutePatternResponse(
                stops = listOf(stop1, stop2),
                routePatterns = listOf(route1rp1, route1rp2).associateBy { it.id },
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(route1rp1.id),
                        stop2.id to listOf(route1rp1.id, route1rp2.id),
                    ),
                routes = mapOf(route1.id to route1)
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
            response.byRouteAndStop(predictions = null)
        )
    }

    @Test
    fun `byRouteAndStop route patterns are sorted by their sort order`() {

        val stop1 = stop()

        val route1 = route()

        val route1rp1 =
            route1.pattern(sortOrder = 1, representativeTrip = trip(headsign = "Harvard"))
        val route1rp2 =
            route1.pattern(sortOrder = 2, representativeTrip = trip(headsign = "Nubian"))

        val response =
            StopAndRoutePatternResponse(
                stops = listOf(stop1),
                routePatterns = listOf(route1rp2, route1rp1).associateBy { it.id },
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(route1rp2.id, route1rp1.id),
                    ),
                routes = mapOf(route1.id to route1)
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
            response.byRouteAndStop(predictions = null)
        )
    }

    @Test
    fun `byRouteAndStop groups patterns by headsign`() {

        val stop1 = stop()

        val route1 = route()

        val route1rp1 =
            route1.pattern(sortOrder = 1, representativeTrip = trip(headsign = "Harvard"))
        val route1rp2 =
            route1.pattern(sortOrder = 1, representativeTrip = trip(headsign = "Harvard"))

        val route1rp3 =
            route1.pattern(sortOrder = 2, representativeTrip = trip(headsign = "Nubian"))

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
            response.byRouteAndStop(predictions = null)
        )
    }

    @Test
    fun `byRouteAndStop when there are no new route patterns for a stop then it is omitted`() {

        val stop1 = stop()
        val stop2 = stop()

        val route1 = route()

        val route1rp1 = route1.pattern(representativeTrip = trip(headsign = "Harvard"))

        val response =
            StopAndRoutePatternResponse(
                stops = listOf(stop1, stop2),
                routePatterns = listOf(route1rp1).associateBy { it.id },
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(route1rp1.id),
                        stop2.id to listOf(route1rp1.id),
                    ),
                routes = mapOf(route1.id to route1)
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
            response.byRouteAndStop(predictions = null)
        )
    }

    @Test
    fun `byRouteAndStop when a stop is served by multiple routes it is included for each route`() {

        val stop1 = stop()
        val stop2 = stop()

        val route1 = route()
        val route2 = route()

        val route1rp1 =
            route1.pattern(sortOrder = 10, representativeTrip = trip(headsign = "Harvard"))
        val route1rp2 =
            route1.pattern(sortOrder = 11, representativeTrip = trip(headsign = "Nubian"))
        val route1rp3 =
            route1.pattern(
                sortOrder = 12,
                representativeTrip = trip(headsign = "Nubian via Allston")
            )

        val route2rp1 =
            route2.pattern(sortOrder = 20, representativeTrip = trip(headsign = "Porter Sq"))

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
                routes = listOf(route1, route2).associateBy { it.id }
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
            response.byRouteAndStop(predictions = null)
        )
    }

    @Test
    fun `byRouteAndStop groups by parent station`() {

        val station1 = stop()

        val station1stop1 = stop(parentStation = station1)
        val station1stop2 = stop(parentStation = station1)

        val stop2 = stop()

        val route1 = route()

        val route1rp1 =
            routePattern(
                sortOrder = 10,
                representativeTrip = trip(headsign = "Harvard"),
                routeId = route1.id
            )
        val route1rp2 =
            routePattern(
                sortOrder = 11,
                representativeTrip = trip(headsign = "Nubian"),
                routeId = route1.id
            )
        val route1rp3 =
            route1.pattern(
                sortOrder = 12,
                representativeTrip = trip(headsign = "Nubian via Allston")
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
                routes = mapOf(route1.id to route1)
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
            response.byRouteAndStop(predictions = null)
        )
    }

    @Test
    fun `byRouteAndStop includes predictions filtered to the correct stop and pattern`() {
        val stop1 = stop()
        val stop2 = stop()

        val route1 = route()

        val pattern1 =
            route1.pattern(sortOrder = 1, representativeTrip = trip(headsign = "Harvard"))
        val pattern2 =
            route1.pattern(sortOrder = 2, representativeTrip = trip(headsign = "Harvard"))
        val pattern3 = route1.pattern(sortOrder = 3, representativeTrip = trip(headsign = "Nubian"))

        val response =
            StopAndRoutePatternResponse(
                stops = listOf(stop1, stop2),
                routePatterns = listOf(pattern1, pattern2, pattern3).associateBy { it.id },
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(pattern1.id, pattern2.id),
                        stop2.id to listOf(pattern1.id, pattern2.id, pattern3.id),
                    ),
                routes = mapOf(route1.id to route1)
            )

        val time = Instant.parse("2024-02-21T09:30:08-05:00")

        // should be sorted before the pattern 1 prediction under Harvard
        val stop1Pattern2Prediction =
            prediction(
                arrivalTime = time,
                departureTime = time + 10.seconds,
                stopId = stop1.id,
                trip = pattern2.trip()
            )

        // should be sorted after the pattern 2 prediction under Harvard
        val stop1Pattern1Prediction =
            prediction(
                arrivalTime = time + 5.seconds,
                departureTime = time + 15.seconds,
                stopId = stop1.id,
                trip = pattern1.trip()
            )

        // should be ignored since pattern 1 shows at stop 1 instead
        val stop2Pattern1Prediction =
            prediction(
                arrivalTime = time + 10.seconds,
                departureTime = time + 20.seconds,
                stopId = stop2.id,
                trip = pattern1.trip()
            )

        // should be shown under Nubian
        val stop2Pattern3Prediction =
            prediction(
                arrivalTime = time + 20.seconds,
                departureTime = time + 30.seconds,
                stopId = stop2.id,
                trip = pattern3.trip()
            )

        assertEquals(
            listOf(
                StopAssociatedRoute(
                    route1,
                    listOf(
                        PatternsByStop(
                            stop1,
                            listOf(
                                PatternsByHeadsign(
                                    "Harvard",
                                    listOf(pattern1, pattern2),
                                    listOf(stop1Pattern2Prediction, stop1Pattern1Prediction)
                                )
                            )
                        ),
                        PatternsByStop(
                            stop2,
                            listOf(
                                PatternsByHeadsign(
                                    "Nubian",
                                    listOf(pattern3),
                                    listOf(stop2Pattern3Prediction)
                                )
                            )
                        )
                    )
                ),
            ),
            response.byRouteAndStop(
                predictions =
                    listOf(
                        stop1Pattern1Prediction,
                        stop1Pattern2Prediction,
                        stop2Pattern1Prediction,
                        stop2Pattern3Prediction
                    )
            )
        )
    }
}
