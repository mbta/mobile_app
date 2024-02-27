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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Instant

class NearbyResponseTest {

    @Test
    fun `NearbyStaticData when a route pattern serves multiple stops it is only included for the first one`() {

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
            NearbyStaticData.build {
                route(route1) {
                    stop(stop1) { headsign("Harvard", listOf(route1rp1)) }
                    stop(stop2) { headsign("Nubian", listOf(route1rp2)) }
                }
            },
            NearbyStaticData(response)
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
            NearbyStaticData.build {
                route(route1) {
                    stop(stop1) {
                        headsign("Harvard", listOf(route1rp1))
                        headsign("Nubian", listOf(route1rp2))
                    }
                }
            },
            NearbyStaticData(response)
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
            NearbyStaticData.build {
                route(route1) {
                    stop(stop1) {
                        headsign("Harvard", listOf(route1rp1, route1rp2))
                        headsign("Nubian", listOf(route1rp3))
                    }
                }
            },
            NearbyStaticData(response)
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
            NearbyStaticData.build {
                route(route1) { stop(stop1) { headsign("Harvard", listOf(route1rp1)) } }
            },
            NearbyStaticData(response)
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
            NearbyStaticData.build {
                route(route1) {
                    stop(stop1) {
                        headsign("Harvard", listOf(route1rp1))
                        headsign("Nubian", listOf(route1rp2))
                    }
                    stop(stop2) { headsign("Nubian via Allston", listOf(route1rp3)) }
                }
                route(route2) { stop(stop2) { headsign("Porter Sq", listOf(route2rp1)) } }
            },
            NearbyStaticData(response)
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
            NearbyStaticData.build {
                route(route1) {
                    stop(station1, listOf(station1stop1.id, station1stop2.id)) {
                        headsign("Harvard", listOf(route1rp1))
                        headsign("Nubian", listOf(route1rp2))
                    }
                    stop(stop2) { headsign("Nubian via Allston", listOf(route1rp3)) }
                }
            },
            NearbyStaticData(response)
        )
    }

    @Test
    fun `withRealtimeInfo includes predictions filtered to the correct stop and pattern`() {
        val stop1 = stop()
        val stop2 = stop()

        val route1 = route()

        val pattern1 =
            route1.pattern(sortOrder = 1, representativeTrip = trip(headsign = "Harvard"))
        val pattern2 =
            route1.pattern(sortOrder = 2, representativeTrip = trip(headsign = "Harvard"))
        val pattern3 = route1.pattern(sortOrder = 3, representativeTrip = trip(headsign = "Nubian"))

        val staticData =
            NearbyStaticData.build {
                route(route1) {
                    stop(stop1) { headsign("Harvard", listOf(pattern1, pattern2)) }
                    stop(stop2) { headsign("Nubian", listOf(pattern3)) }
                }
            }

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
            staticData.withRealtimeInfo(
                sortByDistanceFrom = stop1.position,
                predictions =
                    listOf(
                        stop1Pattern1Prediction,
                        stop1Pattern2Prediction,
                        stop2Pattern1Prediction,
                        stop2Pattern3Prediction
                    ),
                filterAtTime = time
            )
        )
    }

    @Test
    fun `withRealtimeInfo hides rare patterns with no predictions`() {
        val stop1 = stop()

        val route1 = route()

        // should be included because typical and has prediction
        val typicalOutbound =
            route1.pattern(
                directionId = 0,
                sortOrder = 1,
                typicality = RoutePattern.Typicality.Typical,
                representativeTrip = trip(headsign = "Typical Out")
            )
        // should be included because typical
        val typicalInbound =
            route1.pattern(
                directionId = 1,
                sortOrder = 2,
                typicality = RoutePattern.Typicality.Typical,
                representativeTrip = trip(headsign = "Typical In")
            )
        // should be included because prediction within 90 minutes
        val deviationOutbound =
            route1.pattern(
                directionId = 0,
                sortOrder = 3,
                typicality = RoutePattern.Typicality.Deviation,
                representativeTrip = trip(headsign = "Deviation Out")
            )
        // should be included because prediction beyond 90 minutes
        val deviationInbound =
            route1.pattern(
                directionId = 1,
                sortOrder = 4,
                typicality = RoutePattern.Typicality.Deviation,
                representativeTrip = trip(headsign = "Deviation In")
            )
        // should be included because prediction
        val atypicalOutbound =
            route1.pattern(
                directionId = 0,
                sortOrder = 5,
                typicality = RoutePattern.Typicality.Atypical,
                representativeTrip = trip(headsign = "Atypical Out")
            )
        // should be excluded because no prediction
        val atypicalInbound =
            route1.pattern(
                directionId = 1,
                sortOrder = 6,
                typicality = RoutePattern.Typicality.Atypical,
                representativeTrip = trip(headsign = "Atypical In")
            )

        val staticData =
            NearbyStaticData.build {
                route(route1) {
                    stop(stop1) {
                        headsign("Typical Out", listOf(typicalOutbound))
                        headsign("Typical In", listOf(typicalInbound))
                        headsign("Deviation Out", listOf(deviationOutbound))
                        headsign("Deviation In", listOf(deviationInbound))
                        headsign("Atypical Out", listOf(atypicalOutbound))
                        headsign("Atypical In", listOf(atypicalInbound))
                    }
                }
            }

        val time = Instant.parse("2024-02-22T12:08:19-05:00")

        val typicalOutboundPrediction =
            prediction(departureTime = time, stopId = stop1.id, trip = typicalOutbound.trip())
        val deviationOutboundPrediction =
            prediction(
                departureTime = time + 89.minutes,
                stopId = stop1.id,
                trip = deviationOutbound.trip()
            )
        val deviationInboundPrediction =
            prediction(
                departureTime = time + 91.minutes,
                stopId = stop1.id,
                trip = deviationInbound.trip()
            )
        val atypicalInboundPrediction =
            prediction(
                departureTime = time + 1.minutes,
                stopId = stop1.id,
                trip = atypicalInbound.trip()
            )

        val predictions =
            listOf(
                typicalOutboundPrediction,
                deviationOutboundPrediction,
                deviationInboundPrediction,
                atypicalInboundPrediction
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
                                    "Typical Out",
                                    listOf(typicalOutbound),
                                    listOf(typicalOutboundPrediction)
                                ),
                                PatternsByHeadsign("Typical In", listOf(typicalInbound), listOf()),
                                PatternsByHeadsign(
                                    "Deviation Out",
                                    listOf(deviationOutbound),
                                    listOf(deviationOutboundPrediction)
                                ),
                                PatternsByHeadsign(
                                    "Atypical In",
                                    listOf(atypicalInbound),
                                    listOf(atypicalInboundPrediction)
                                )
                            )
                        )
                    )
                )
            ),
            staticData.withRealtimeInfo(
                sortByDistanceFrom = stop1.position,
                predictions = predictions,
                filterAtTime = time
            )
        )
    }

    @Test
    fun `withRealtimeInfo handles parent stops`() {
        val parentStop = stop()
        val childStop = stop(parentStation = parentStop)
        val route1 = route()
        val pattern1 = route1.pattern()

        val staticData =
            NearbyStaticData.build {
                route(route1) {
                    stop(parentStop, listOf(childStop.id)) { headsign("Harvard", listOf(pattern1)) }
                }
            }

        val time = Instant.parse("2024-02-26T10:45:38-05:00")

        val prediction1 =
            prediction(departureTime = time, stopId = childStop.id, trip = pattern1.trip())

        assertEquals(
            listOf(
                StopAssociatedRoute(
                    route1,
                    listOf(
                        PatternsByStop(
                            parentStop,
                            listOf(
                                PatternsByHeadsign("Harvard", listOf(pattern1), listOf(prediction1))
                            )
                        )
                    )
                )
            ),
            staticData.withRealtimeInfo(
                sortByDistanceFrom = parentStop.position,
                predictions = listOf(prediction1),
                filterAtTime = time
            )
        )
    }
}
