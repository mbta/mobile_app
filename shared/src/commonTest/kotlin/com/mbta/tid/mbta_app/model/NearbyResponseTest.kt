package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Instant

class NearbyResponseTest {

    @Test
    fun `NearbyStaticData when a route pattern serves multiple stops it is only included for the first one`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()
        val stop2 = objects.stop()

        val route1 = objects.route()

        val route1rp1 = objects.routePattern(route1) { representativeTrip { headsign = "Harvard" } }
        val route1rp2 = objects.routePattern(route1) { representativeTrip { headsign = "Nubian" } }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(route1rp1.id),
                        stop2.id to listOf(route1rp1.id, route1rp2.id),
                    ),
            )
        val nearby = NearbyResponse(objects)

        assertEquals(
            NearbyStaticData.build {
                route(route1) {
                    stop(stop1) { headsign("Harvard", listOf(route1rp1)) }
                    stop(stop2) { headsign("Nubian", listOf(route1rp2)) }
                }
            },
            NearbyStaticData(global, nearby)
        )
    }

    @Test
    fun `byRouteAndStop route patterns are sorted by their sort order`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()

        val route1 = objects.route()

        val route1rp1 =
            objects.routePattern(route1) {
                sortOrder = 1
                representativeTrip { headsign = "Harvard" }
            }
        val route1rp2 =
            objects.routePattern(route1) {
                sortOrder = 2
                representativeTrip { headsign = "Nubian" }
            }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(route1rp2.id, route1rp1.id),
                    ),
            )
        val nearby = NearbyResponse(objects)

        assertEquals(
            NearbyStaticData.build {
                route(route1) {
                    stop(stop1) {
                        headsign("Harvard", listOf(route1rp1))
                        headsign("Nubian", listOf(route1rp2))
                    }
                }
            },
            NearbyStaticData(global, nearby)
        )
    }

    @Test
    fun `byRouteAndStop sorts subway routes first`() {
        val objects = ObjectCollectionBuilder()

        val busStop = objects.stop()
        val subwayStop = objects.stop()

        val subwayRoute = objects.route { type = RouteType.LIGHT_RAIL }
        val busRoute = objects.route { type = RouteType.BUS }

        val subwayRp =
            objects.routePattern(subwayRoute) {
                sortOrder = 2
                representativeTrip { headsign = "Alewife" }
            }
        val busRp =
            objects.routePattern(busRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Nubian" }
            }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        subwayStop.id to listOf(subwayRp.id, busRp.id),
                        busStop.id to listOf(busRp.id),
                    ),
            )
        val nearby = NearbyResponse(objects)

        assertEquals(
            NearbyStaticData.build {
                route(subwayRoute) { stop(subwayStop) { headsign("Alewife", listOf(subwayRp)) } }
                route(busRoute) { stop(busStop) { headsign("Nubian", listOf(busRp)) } }
            },
            NearbyStaticData(global, nearby)
        )
    }

    @Test
    fun `byRouteAndStop preserves original stop ordering among subway routes`() {
        val objects = ObjectCollectionBuilder()

        val closerStop = objects.stop()
        val furtherStop = objects.stop()

        val subwayRoute1 = objects.route { type = RouteType.LIGHT_RAIL }
        val subwayRoute2 = objects.route { type = RouteType.LIGHT_RAIL }

        val subway1Rp1 =
            objects.routePattern(subwayRoute1) {
                sortOrder = 2
                representativeTrip { headsign = "Alewife" }
            }
        val subway2Rp1 =
            objects.routePattern(subwayRoute2) {
                sortOrder = 1
                representativeTrip { headsign = "Braintree" }
            }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        closerStop.id to listOf(subway2Rp1.id),
                        furtherStop.id to listOf(subway1Rp1.id),
                    ),
            )
        val nearby = NearbyResponse(objects)

        assertEquals(
            NearbyStaticData.build {
                route(subwayRoute2) {
                    stop(closerStop) { headsign("Braintree", listOf(subway2Rp1)) }
                }
                route(subwayRoute1) {
                    stop(furtherStop) { headsign("Alewife", listOf(subway1Rp1)) }
                }
            },
            NearbyStaticData(global, nearby)
        )
    }

    @Test
    fun `byRouteAndStop groups patterns by headsign`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()

        val route1 = objects.route()

        val route1rp1 =
            objects.routePattern(route1) {
                sortOrder = 1
                representativeTrip { headsign = "Harvard" }
            }
        val route1rp2 =
            objects.routePattern(route1) {
                sortOrder = 1
                representativeTrip { headsign = "Harvard" }
            }

        val route1rp3 =
            objects.routePattern(route1) {
                sortOrder = 2
                representativeTrip { headsign = "Nubian" }
            }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(route1rp1.id, route1rp2.id, route1rp3.id),
                    )
            )
        val nearby = NearbyResponse(objects)

        assertEquals(
            NearbyStaticData.build {
                route(route1) {
                    stop(stop1) {
                        headsign("Harvard", listOf(route1rp1, route1rp2))
                        headsign("Nubian", listOf(route1rp3))
                    }
                }
            },
            NearbyStaticData(global, nearby)
        )
    }

    @Test
    fun `byRouteAndStop when there are no new route patterns for a stop then it is omitted`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()
        val stop2 = objects.stop()

        val route1 = objects.route()

        val route1rp1 = objects.routePattern(route1) { representativeTrip { headsign = "Harvard" } }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(route1rp1.id),
                        stop2.id to listOf(route1rp1.id),
                    )
            )
        val nearby = NearbyResponse(objects)

        assertEquals(
            NearbyStaticData.build {
                route(route1) { stop(stop1) { headsign("Harvard", listOf(route1rp1)) } }
            },
            NearbyStaticData(global, nearby)
        )
    }

    @Test
    fun `byRouteAndStop when a stop is served by multiple routes it is included for each route`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()
        val stop2 = objects.stop()

        val route1 = objects.route()
        val route2 = objects.route()

        val route1rp1 =
            objects.routePattern(route1) {
                sortOrder = 10
                representativeTrip { headsign = "Harvard" }
            }
        val route1rp2 =
            objects.routePattern(route1) {
                sortOrder = 11
                representativeTrip { headsign = "Nubian" }
            }
        val route1rp3 =
            objects.routePattern(route1) {
                sortOrder = 12
                representativeTrip { headsign = "Nubian via Allston" }
            }

        val route2rp1 =
            objects.routePattern(route2) {
                sortOrder = 20
                representativeTrip { headsign = "Porter Sq" }
            }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(route1rp1.id, route1rp2.id),
                        stop2.id to listOf(route1rp1.id, route1rp3.id, route2rp1.id),
                    ),
            )
        val nearby = NearbyResponse(objects)

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
            NearbyStaticData(global, nearby)
        )
    }

    @Test
    fun `byRouteAndStop groups by parent station`() {
        val objects = ObjectCollectionBuilder()

        val station1 = objects.stop()

        val station1stop1 = objects.stop { parentStationId = station1.id }
        val station1stop2 = objects.stop { parentStationId = station1.id }

        val stop2 = objects.stop()

        val route1 = objects.route()

        val route1rp1 =
            objects.routePattern(route1) {
                sortOrder = 10
                representativeTrip { headsign = "Harvard" }
            }
        val route1rp2 =
            objects.routePattern(route1) {
                sortOrder = 11
                representativeTrip { headsign = "Nubian" }
            }
        val route1rp3 =
            objects.routePattern(route1) {
                sortOrder = 12
                representativeTrip { headsign = "Nubian via Allston" }
            }

        val global =
            GlobalResponse(
                objects,
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
            )
        val nearby = NearbyResponse(objects)

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
            NearbyStaticData(global, nearby)
        )
    }

    @Test
    fun `byRouteAndStop preserves unscheduled physical platform`() {
        val objects = ObjectCollectionBuilder()

        val parentStation = objects.stop { id = "place-forhl" }

        val logicalPlatform =
            objects.stop {
                id = "70001"
                parentStationId = parentStation.id
            }
        val physicalPlatform =
            objects.stop {
                id = "Forest Hills-01"
                parentStationId = parentStation.id
            }

        val route = objects.route { id = "Orange" }

        val routePattern =
            objects.routePattern(route) { representativeTrip { headsign = "Oak Grove" } }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        logicalPlatform.id to
                            listOf(
                                routePattern.id,
                            ),
                    ),
            )
        val nearby = NearbyResponse(objects)

        assertEquals(
            NearbyStaticData.build {
                route(route) {
                    stop(parentStation, listOf(logicalPlatform.id, physicalPlatform.id)) {
                        headsign("Oak Grove", listOf(routePattern))
                    }
                }
            },
            NearbyStaticData(global, nearby)
        )
    }

    @Test
    fun `Green Line routes are grouped together`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()
        val stop2 = objects.stop()
        val stop3 = objects.stop()

        val line = objects.line { id = "line-Green" }

        val route1 =
            objects.route {
                lineId = line.id
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Boston College", "Park St & North")
            }
        val route2 =
            objects.route {
                lineId = line.id
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Cleveland Circle", "Park St & North")
            }

        val route1rp1 =
            objects.routePattern(route1) {
                representativeTrip { headsign = "Boston College" }
                directionId = 0
                sortOrder = 1
            }
        val route1rp2 =
            objects.routePattern(route1) {
                representativeTrip { headsign = "Government Center" }
                directionId = 1
                sortOrder = 2
            }
        val route2rp1 =
            objects.routePattern(route2) {
                representativeTrip { headsign = "Cleveland Circle" }
                directionId = 0
                sortOrder = 3
            }
        val route2rp2 =
            objects.routePattern(route2) {
                representativeTrip { headsign = "Government Center" }
                directionId = 1
                sortOrder = 4
            }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(route1rp1.id),
                        stop2.id to listOf(route2rp1.id),
                        stop3.id to listOf(route1rp2.id, route2rp2.id),
                    ),
            )
        val nearby = NearbyResponse(objects)

        val cDir = Direction("West", "Cleveland Circle", 0)
        val bDir = Direction("West", "Boston College", 0)
        val parkDir = Direction("East", "Park St & North", 1)

        assertEquals(
            NearbyStaticData.build {
                line(line, listOf(route1, route2)) {
                    stop(stop1, routes = listOf(route1), directions = listOf(bDir, parkDir)) {
                        headsign(route1, "Boston College", listOf(route1rp1))
                    }
                    stop(
                        stop3,
                        routes = listOf(route1, route2),
                        directions = listOf(bDir, parkDir)
                    ) {
                        direction(parkDir, listOf(route1, route2), listOf(route1rp2, route2rp2))
                    }
                    stop(stop2, listOf(route2), directions = listOf(cDir, parkDir)) {
                        headsign(route2, "Cleveland Circle", listOf(route2rp1))
                    }
                }
            },
            NearbyStaticData(global, nearby)
        )
    }

    @Test
    fun `withRealtimeInfo includes predictions filtered to the correct stop and pattern`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()
        val stop2 = objects.stop()

        val route1 = objects.route()

        val pattern1 =
            objects.routePattern(route1) {
                sortOrder = 1
                representativeTrip { headsign = "Harvard" }
            }
        val trip1 = objects.trip(pattern1)
        val pattern2 =
            objects.routePattern(route1) {
                sortOrder = 2
                representativeTrip { headsign = "Harvard" }
            }
        val trip2 = objects.trip(pattern2)
        val pattern3 =
            objects.routePattern(route1) {
                sortOrder = 3
                representativeTrip { headsign = "Nubian" }
            }
        val trip3 = objects.trip(pattern3)

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
            objects.prediction {
                arrivalTime = time
                departureTime = time + 10.seconds
                stopId = stop1.id
                trip = trip2
            }

        // should be sorted after the pattern 2 prediction under Harvard
        val stop1Pattern1Prediction =
            objects.prediction {
                arrivalTime = time + 5.seconds
                departureTime = time + 15.seconds
                stopId = stop1.id
                trip = trip1
            }

        // should be ignored since pattern 1 shows at stop 1 instead
        val stop2Pattern1Prediction =
            objects.prediction {
                arrivalTime = time + 10.seconds
                departureTime = time + 20.seconds
                stopId = stop2.id
                trip = trip1
            }

        // should be shown under Nubian
        val stop2Pattern3Prediction =
            objects.prediction {
                arrivalTime = time + 20.seconds
                departureTime = time + 30.seconds
                stopId = stop2.id
                trip = trip3
            }

        assertEquals(
            listOf(
                StopsAssociated.WithRoute(
                    route1,
                    listOf(
                        PatternsByStop(
                            route1,
                            stop1,
                            listOf(
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "Harvard",
                                    null,
                                    listOf(pattern1, pattern2),
                                    listOf(
                                        objects.upcomingTrip(stop1Pattern2Prediction),
                                        objects.upcomingTrip(stop1Pattern1Prediction)
                                    )
                                )
                            )
                        ),
                        PatternsByStop(
                            route1,
                            stop2,
                            listOf(
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "Nubian",
                                    null,
                                    listOf(pattern3),
                                    listOf(objects.upcomingTrip(stop2Pattern3Prediction))
                                )
                            )
                        )
                    )
                ),
            ),
            staticData.withRealtimeInfo(
                sortByDistanceFrom = stop1.position,
                schedules = null,
                predictions = PredictionsStreamDataResponse(objects),
                alerts = null,
                filterAtTime = time,
                pinnedRoutes = setOf(),
            )
        )
    }

    @Test
    fun `withRealtimeInfo hides rare patterns with no predictions`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()

        val route1 = objects.route()

        // should be included because typical and has prediction
        val typicalOutbound =
            objects.routePattern(route1) {
                directionId = 0
                sortOrder = 1
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Typical Out" }
            }
        // should be included because typical
        val typicalInbound =
            objects.routePattern(route1) {
                directionId = 1
                sortOrder = 2
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Typical In" }
            }
        // should be included because prediction within 90 minutes
        val deviationOutbound =
            objects.routePattern(route1) {
                directionId = 0
                sortOrder = 3
                typicality = RoutePattern.Typicality.Deviation
                representativeTrip { headsign = "Deviation Out" }
            }
        // should be included because prediction beyond 90 minutes
        val deviationInbound =
            objects.routePattern(route1) {
                directionId = 1
                sortOrder = 4
                typicality = RoutePattern.Typicality.Deviation
                representativeTrip { headsign = "Deviation In" }
            }
        // should be included because prediction
        val atypicalOutbound =
            objects.routePattern(route1) {
                directionId = 0
                sortOrder = 5
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { headsign = "Atypical Out" }
            }
        // should be excluded because no prediction
        val atypicalInbound =
            objects.routePattern(route1) {
                directionId = 1
                sortOrder = 6
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { headsign = "Atypical In" }
            }

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
            objects.prediction {
                departureTime = time
                routeId = route1.id
                stopId = stop1.id
                tripId = typicalOutbound.representativeTripId
            }
        val deviationOutboundPrediction =
            objects.prediction {
                departureTime = time + 89.minutes
                routeId = route1.id
                stopId = stop1.id
                tripId = deviationOutbound.representativeTripId
            }
        val deviationInboundPrediction =
            objects.prediction {
                departureTime = time + 91.minutes
                routeId = route1.id
                stopId = stop1.id
                tripId = deviationInbound.representativeTripId
            }
        val atypicalInboundPrediction =
            objects.prediction {
                departureTime = time + 1.minutes
                routeId = route1.id
                stopId = stop1.id
                tripId = atypicalInbound.representativeTripId
            }

        assertEquals(
            listOf(
                StopsAssociated.WithRoute(
                    route1,
                    listOf(
                        PatternsByStop(
                            route1,
                            stop1,
                            listOf(
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "Typical Out",
                                    null,
                                    listOf(typicalOutbound),
                                    listOf(objects.upcomingTrip(typicalOutboundPrediction))
                                ),
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "Typical In",
                                    null,
                                    listOf(typicalInbound),
                                    emptyList()
                                ),
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "Deviation Out",
                                    null,
                                    listOf(deviationOutbound),
                                    listOf(objects.upcomingTrip(deviationOutboundPrediction))
                                ),
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "Atypical In",
                                    null,
                                    listOf(atypicalInbound),
                                    listOf(objects.upcomingTrip(atypicalInboundPrediction))
                                )
                            )
                        )
                    )
                )
            ),
            staticData.withRealtimeInfo(
                sortByDistanceFrom = stop1.position,
                schedules = null,
                predictions = PredictionsStreamDataResponse(objects),
                alerts = null,
                filterAtTime = time,
                pinnedRoutes = setOf(),
            )
        )
    }

    @Test
    fun `withRealtimeInfo hides rare patterns while loading`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()

        val route1 = objects.route()

        // should be included because typical and has prediction
        val typicalOutbound =
            objects.routePattern(route1) {
                directionId = 0
                sortOrder = 1
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Typical Out" }
            }
        // should be included because typical
        val typicalInbound =
            objects.routePattern(route1) {
                directionId = 1
                sortOrder = 2
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Typical In" }
            }
        // should be included because prediction within 90 minutes
        val deviationOutbound =
            objects.routePattern(route1) {
                directionId = 0
                sortOrder = 3
                typicality = RoutePattern.Typicality.Deviation
                representativeTrip { headsign = "Deviation Out" }
            }
        // should be included because prediction beyond 90 minutes
        val deviationInbound =
            objects.routePattern(route1) {
                directionId = 1
                sortOrder = 4
                typicality = RoutePattern.Typicality.Deviation
                representativeTrip { headsign = "Deviation In" }
            }
        // should be included because prediction
        val atypicalOutbound =
            objects.routePattern(route1) {
                directionId = 0
                sortOrder = 5
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { headsign = "Atypical Out" }
            }
        // should be excluded because no prediction
        val atypicalInbound =
            objects.routePattern(route1) {
                directionId = 1
                sortOrder = 6
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { headsign = "Atypical In" }
            }

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

        assertEquals(
            listOf(
                StopsAssociated.WithRoute(
                    route1,
                    listOf(
                        PatternsByStop(
                            route1,
                            stop1,
                            listOf(
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "Typical Out",
                                    null,
                                    listOf(typicalOutbound),
                                    emptyList()
                                ),
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "Typical In",
                                    null,
                                    listOf(typicalInbound),
                                    emptyList()
                                ),
                            )
                        )
                    )
                )
            ),
            staticData.withRealtimeInfo(
                sortByDistanceFrom = stop1.position,
                schedules = null,
                predictions = null,
                alerts = null,
                filterAtTime = time,
                pinnedRoutes = setOf(),
            )
        )
    }

    @Test
    fun `withRealtimeInfo sorts subway first then by distance`() {
        val objects = ObjectCollectionBuilder()

        val closeBusStop = objects.stop()
        val farBusStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.1
                longitude = closeBusStop.longitude + 0.1
            }
        val closeSubwayStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.2
                longitude = closeBusStop.longitude + 0.2
            }
        val farSubwayStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.3
                longitude = closeBusStop.longitude + 0.3
            }

        val closeBusRoute =
            objects.route {
                type = RouteType.BUS
                sortOrder = 4
            }
        val farBusRoute =
            objects.route {
                type = RouteType.BUS
                sortOrder = 3
            }
        val closeSubwayRoute =
            objects.route {
                type = RouteType.LIGHT_RAIL
                sortOrder = 2
            }
        val farSubwayRoute =
            objects.route {
                type = RouteType.LIGHT_RAIL
                sortOrder = 1
            }

        val closeSubwayPattern =
            objects.routePattern(closeSubwayRoute) { representativeTrip { headsign = "Alewife" } }
        val farSubwayPattern =
            objects.routePattern(farSubwayRoute) { representativeTrip { headsign = "Oak Grove" } }
        val closeBusPattern =
            objects.routePattern(closeBusRoute) { representativeTrip { headsign = "Nubian" } }
        val farBusPattern =
            objects.routePattern(farBusRoute) { representativeTrip { headsign = "Malden Center" } }

        val staticData =
            NearbyStaticData.build {
                route(farBusRoute) {
                    stop(farBusStop) { headsign("Malden Center", listOf(farBusPattern)) }
                }
                route(closeBusRoute) {
                    stop(closeBusStop) { headsign("Nubian", listOf(closeBusPattern)) }
                }
                route(farSubwayRoute) {
                    stop(farSubwayStop) { headsign("Oak Grove", listOf(farSubwayPattern)) }
                }
                route(closeSubwayRoute) {
                    stop(closeSubwayStop) { headsign("Alewife", listOf(closeSubwayPattern)) }
                }
            }

        val time = Instant.parse("2024-02-21T09:30:08-05:00")

        // close subway prediction
        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = closeSubwayRoute.id
            stopId = closeSubwayStop.id
            tripId = closeSubwayPattern.representativeTripId
        }

        // far subway prediction
        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = farSubwayRoute.id
            stopId = farSubwayStop.id
            tripId = farSubwayPattern.representativeTripId
        }

        // close bus prediction
        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = closeBusRoute.id
            stopId = closeBusStop.id
            tripId = closeBusPattern.representativeTripId
        }

        // far bus prediction
        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = farBusRoute.id
            stopId = farBusStop.id
            tripId = farBusPattern.representativeTripId
        }

        val realtimeRoutesSorted =
            staticData.withRealtimeInfo(
                sortByDistanceFrom = closeBusStop.position,
                predictions = PredictionsStreamDataResponse(objects),
                schedules = ScheduleResponse(objects),
                alerts = null,
                filterAtTime = time,
                pinnedRoutes = setOf(),
            )
        assertEquals(
            listOf(closeSubwayRoute, farSubwayRoute, closeBusRoute, farBusRoute),
            realtimeRoutesSorted.flatMap {
                when (it) {
                    is StopsAssociated.WithRoute -> listOf(it.route)
                    is StopsAssociated.WithLine -> it.routes
                }
            }
        )
    }

    @Test
    fun `withRealtimeInfo sorts pinned routes to the top`() {
        val objects = ObjectCollectionBuilder()

        val closeBusStop = objects.stop()
        val farBusStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.1
                longitude = closeBusStop.longitude + 0.1
            }
        val closeSubwayStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.2
                longitude = closeBusStop.longitude + 0.2
            }
        val farSubwayStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.3
                longitude = closeBusStop.longitude + 0.3
            }

        val closeBusRoute = objects.route { type = RouteType.BUS }
        val farBusRoute = objects.route { type = RouteType.BUS }
        val closeSubwayRoute = objects.route { type = RouteType.LIGHT_RAIL }
        val farSubwayRoute = objects.route { type = RouteType.LIGHT_RAIL }

        val closeSubwayPattern =
            objects.routePattern(closeSubwayRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Alewife" }
            }
        val farSubwayPattern =
            objects.routePattern(farSubwayRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Oak Grove" }
            }
        val closeBusPattern =
            objects.routePattern(closeBusRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Nubian" }
            }
        val farBusPattern =
            objects.routePattern(farBusRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Malden Center" }
            }

        val staticData =
            NearbyStaticData.build {
                route(farBusRoute) {
                    stop(farBusStop) { headsign("Malden Center", listOf(farBusPattern)) }
                }
                route(closeBusRoute) {
                    stop(closeBusStop) { headsign("Nubian", listOf(closeBusPattern)) }
                }
                route(farSubwayRoute) {
                    stop(farSubwayStop) { headsign("Oak Grove", listOf(farSubwayPattern)) }
                }
                route(closeSubwayRoute) {
                    stop(closeSubwayStop) { headsign("Alewife", listOf(closeSubwayPattern)) }
                }
            }

        val time = Instant.parse("2024-02-21T09:30:08-05:00")

        // close subway prediction
        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = closeSubwayRoute.id
            stopId = closeSubwayStop.id
            tripId = closeSubwayPattern.representativeTripId
        }

        // far subway prediction
        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = farSubwayRoute.id
            stopId = farSubwayStop.id
            tripId = farSubwayPattern.representativeTripId
        }

        // close bus prediction
        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = closeBusRoute.id
            stopId = closeBusStop.id
            tripId = closeBusPattern.representativeTripId
        }

        // far bus prediction
        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = farBusRoute.id
            stopId = farBusStop.id
            tripId = farBusPattern.representativeTripId
        }

        val realtimeRoutesSorted =
            staticData.withRealtimeInfo(
                sortByDistanceFrom = closeBusStop.position,
                predictions = PredictionsStreamDataResponse(objects),
                schedules = ScheduleResponse(objects),
                alerts = null,
                filterAtTime = time,
                pinnedRoutes = setOf(farBusRoute.id, farSubwayRoute.id),
            )
        assertEquals(
            listOf(farSubwayRoute, farBusRoute, closeSubwayRoute, closeBusRoute),
            realtimeRoutesSorted.flatMap {
                when (it) {
                    is StopsAssociated.WithRoute -> listOf(it.route)
                    is StopsAssociated.WithLine -> it.routes
                }
            }
        )
    }

    @Test
    fun `withRealtimeInfo handles parent stops`() {
        val objects = ObjectCollectionBuilder()
        val parentStop = objects.stop()
        val childStop = objects.stop { parentStationId = parentStop.id }
        val route1 = objects.route()
        val pattern1 = objects.routePattern(route1) { representativeTrip { headsign = "Harvard" } }

        val staticData =
            NearbyStaticData.build {
                route(route1) {
                    stop(parentStop, listOf(childStop.id)) { headsign("Harvard", listOf(pattern1)) }
                }
            }

        val time = Instant.parse("2024-02-26T10:45:38-05:00")

        val prediction1 =
            objects.prediction {
                departureTime = time
                routeId = route1.id
                stopId = childStop.id
                tripId = pattern1.representativeTripId
            }

        assertEquals(
            listOf(
                StopsAssociated.WithRoute(
                    route1,
                    listOf(
                        PatternsByStop(
                            route1,
                            parentStop,
                            listOf(
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "Harvard",
                                    null,
                                    listOf(pattern1),
                                    listOf(objects.upcomingTrip(prediction1))
                                )
                            )
                        )
                    )
                )
            ),
            staticData.withRealtimeInfo(
                sortByDistanceFrom = parentStop.position,
                schedules = null,
                predictions = PredictionsStreamDataResponse(objects),
                alerts = null,
                filterAtTime = time,
                pinnedRoutes = setOf(),
            )
        )
    }

    @Test
    fun `withRealtimeInfo incorporates schedules`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route()
        val routePattern = objects.routePattern(route) { representativeTrip { headsign = "A" } }
        val trip1 = objects.trip(routePattern)
        val trip2 = objects.trip(routePattern)

        val time = Instant.parse("2024-03-14T12:23:44-04:00")

        val sched1 =
            objects.schedule {
                trip = trip1
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 1.minutes
            }
        val sched2 =
            objects.schedule {
                trip = trip2
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 2.minutes
            }

        val pred1 = objects.prediction(sched1) { departureTime = time + 1.5.minutes }
        val pred2 = objects.prediction(sched2) { departureTime = null }

        val staticData =
            NearbyStaticData.build {
                route(route) { stop(stop) { headsign("A", listOf(routePattern)) } }
            }

        assertEquals(
            listOf(
                StopsAssociated.WithRoute(
                    route,
                    listOf(
                        PatternsByStop(
                            route,
                            stop,
                            listOf(
                                RealtimePatterns.ByHeadsign(
                                    route,
                                    "A",
                                    null,
                                    listOf(routePattern),
                                    listOf(
                                        objects.upcomingTrip(sched1, pred1),
                                        objects.upcomingTrip(sched2, pred2)
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            staticData.withRealtimeInfo(
                sortByDistanceFrom = stop.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = null,
                filterAtTime = time,
                pinnedRoutes = setOf(),
            )
        )
    }

    @Test
    fun `withRealtimeInfo checks route along with headsign and stop`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route1 = objects.route { sortOrder = 1 }
        val routePattern1 = objects.routePattern(route1) { representativeTrip { headsign = "A" } }
        val trip1 = objects.trip(routePattern1)

        val route2 = objects.route { sortOrder = 2 }
        val routePattern2 = objects.routePattern(route2) { representativeTrip { headsign = "A" } }
        val trip2 = objects.trip(routePattern2)

        val time = Instant.parse("2024-03-18T10:41:13-04:00")

        val sched1 =
            objects.schedule {
                trip = trip1
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 1.minutes
            }
        val sched2 =
            objects.schedule {
                trip = trip2
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 2.minutes
            }

        val pred1 = objects.prediction(sched1) { departureTime = time + 1.5.minutes }
        val pred2 = objects.prediction(sched2) { departureTime = time + 2.3.minutes }

        val staticData =
            NearbyStaticData.build {
                route(route1) { stop(stop) { headsign("A", listOf(routePattern1)) } }
                route(route2) { stop(stop) { headsign("A", listOf(routePattern2)) } }
            }

        assertEquals(
            listOf(
                StopsAssociated.WithRoute(
                    route1,
                    listOf(
                        PatternsByStop(
                            route1,
                            stop,
                            listOf(
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "A",
                                    null,
                                    listOf(routePattern1),
                                    listOf(objects.upcomingTrip(sched1, pred1))
                                )
                            )
                        )
                    )
                ),
                StopsAssociated.WithRoute(
                    route2,
                    listOf(
                        PatternsByStop(
                            route2,
                            stop,
                            listOf(
                                RealtimePatterns.ByHeadsign(
                                    route2,
                                    "A",
                                    null,
                                    listOf(routePattern2),
                                    listOf(objects.upcomingTrip(sched2, pred2))
                                )
                            )
                        )
                    )
                )
            ),
            staticData.withRealtimeInfo(
                sortByDistanceFrom = stop.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = null,
                filterAtTime = time,
                pinnedRoutes = setOf(),
            )
        )
    }

    @Test
    fun `withRealtimeInfo picks out alerts`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route { sortOrder = 1 }
        val routePattern =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "A" }
            }

        val time = Instant.parse("2024-03-19T14:16:17-04:00")

        val alert =
            objects.alert {
                activePeriod(
                    Instant.parse("2024-03-18T04:30:00-04:00"),
                    Instant.parse("2024-03-22T02:30:00-04:00")
                )
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride
                    ),
                    route = route.id,
                    routeType = route.type,
                    stop = stop.id
                )
            }

        val staticData =
            NearbyStaticData.build {
                route(route) { stop(stop) { headsign("A", listOf(routePattern)) } }
            }

        assertEquals(
            listOf(
                StopsAssociated.WithRoute(
                    route,
                    listOf(
                        PatternsByStop(
                            route,
                            stop,
                            listOf(
                                RealtimePatterns.ByHeadsign(
                                    route,
                                    "A",
                                    null,
                                    listOf(routePattern),
                                    emptyList(),
                                    alertsHere = listOf(alert)
                                )
                            )
                        )
                    )
                )
            ),
            staticData.withRealtimeInfo(
                sortByDistanceFrom = stop.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(objects),
                filterAtTime = time,
                pinnedRoutes = setOf(),
            )
        )
    }

    @Test
    fun `withRealtimeInfo hides headsigns that are arrival-only`() {

        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route()
        val routePattern1 = objects.routePattern(route) { representativeTrip { headsign = "A" } }
        val routePattern2 = objects.routePattern(route) { representativeTrip { headsign = "B" } }
        val trip1 = objects.trip(routePattern1)
        val trip2 = objects.trip(routePattern2)

        val time = Instant.parse("2024-03-14T12:23:44-04:00")

        val sched1 =
            objects.schedule {
                trip = trip1
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 1.minutes
            }
        val sched2 =
            objects.schedule {
                trip = trip2
                stopId = stop.id
                stopSequence = 90
                arrivalTime = time + 2.minutes
                departureTime = null
                pickUpType = Schedule.StopEdgeType.Unavailable
            }

        val staticData =
            NearbyStaticData.build {
                route(route) {
                    stop(stop) {
                        headsign("A", listOf(routePattern1))
                        headsign("B", listOf(routePattern2))
                    }
                }
            }

        assertEquals(
            listOf(
                StopsAssociated.WithRoute(
                    route,
                    listOf(
                        PatternsByStop(
                            route,
                            stop,
                            listOf(
                                RealtimePatterns.ByHeadsign(
                                    route,
                                    "A",
                                    null,
                                    listOf(routePattern1),
                                    listOf(objects.upcomingTrip(sched1))
                                )
                            )
                        )
                    )
                )
            ),
            staticData.withRealtimeInfo(
                sortByDistanceFrom = stop.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = null,
                filterAtTime = time,
                pinnedRoutes = setOf(),
            )
        )
    }

    @Test
    fun `withRealtimeInfo groups lines by direction`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val line = objects.line { id = "line-Green" }
        val routeB =
            objects.route {
                id = "B"
                sortOrder = 1
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Kenmore & West", "Park St & North")
            }
        val routePatternB1 =
            objects.routePattern(routeB) {
                representativeTrip { headsign = "B" }
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val routePatternB2 =
            objects.routePattern(routeB) {
                representativeTrip { headsign = "B" }
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
            }
        val tripB1 = objects.trip(routePatternB1)
        val tripB2 = objects.trip(routePatternB2)

        val routeC =
            objects.route {
                id = "C"
                sortOrder = 2
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Kenmore & West", "Park St & North")
            }
        val routePatternC1 =
            objects.routePattern(routeC) {
                representativeTrip { headsign = "C" }
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val routePatternC2 =
            objects.routePattern(routeC) {
                representativeTrip { headsign = "C" }
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
            }
        val tripC1 = objects.trip(routePatternC1)
        val tripC2 = objects.trip(routePatternC2)

        val routeE =
            objects.route {
                id = "E"
                sortOrder = 3
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Heath Street", "Park St & North")
            }
        val routePatternE1 =
            objects.routePattern(routeE) {
                representativeTrip { headsign = "Heath Street" }
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val routePatternE2 =
            objects.routePattern(routeE) {
                representativeTrip { headsign = "Medford/Tufts" }
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
            }
        val tripE1 = objects.trip(routePatternE1)
        val tripE2 = objects.trip(routePatternE2)

        val time = Instant.parse("2024-03-18T10:41:13-04:00")

        val schedB1 =
            objects.schedule {
                trip = tripB1
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 1.minutes
            }
        val schedB2 =
            objects.schedule {
                trip = tripB2
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 4.minutes
            }
        val schedC1 =
            objects.schedule {
                trip = tripC1
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 2.minutes
            }
        val schedC2 =
            objects.schedule {
                trip = tripC2
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 5.minutes
            }
        val schedE1 =
            objects.schedule {
                trip = tripE1
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 3.minutes
            }
        val schedE2 =
            objects.schedule {
                trip = tripE2
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 6.minutes
            }

        val predB1 = objects.prediction(schedB1) { departureTime = time + 1.5.minutes }
        val predB2 = objects.prediction(schedB2) { departureTime = time + 4.5.minutes }
        val predC1 = objects.prediction(schedC1) { departureTime = time + 2.3.minutes }
        val predC2 = objects.prediction(schedC2) { departureTime = time + 5.3.minutes }
        val predE1 = objects.prediction(schedE1) { departureTime = time + 2.3.minutes }
        val predE2 = objects.prediction(schedE2) { departureTime = time + 6.3.minutes }

        val directionWest = Direction("West", "Kenmore & West", 0)
        val directionEast = Direction("East", "Park St & North", 1)

        val staticData =
            NearbyStaticData.build {
                line(line, listOf(routeB, routeC, routeE)) {
                    stop(stop, listOf(routeB, routeC, routeE)) {
                        direction(
                            directionWest,
                            listOf(routeB, routeC),
                            listOf(routePatternB1, routePatternC1)
                        )
                        headsign(routeE, "Heath Street", listOf(routePatternE1))
                        direction(
                            directionEast,
                            listOf(routeB, routeC, routeE),
                            listOf(routePatternB2, routePatternC2, routePatternE2)
                        )
                    }
                }
            }

        val expected =
            StopsAssociated.WithLine(
                line,
                listOf(routeB, routeC, routeE),
                listOf(
                    PatternsByStop(
                        listOf(routeB, routeC, routeE),
                        line,
                        stop,
                        listOf(
                            RealtimePatterns.ByDirection(
                                line,
                                listOf(routeB, routeC),
                                directionWest,
                                listOf(routePatternB1, routePatternC1),
                                listOf(
                                    objects.upcomingTrip(schedB1, predB1),
                                    objects.upcomingTrip(schedC1, predC1),
                                )
                            ),
                            RealtimePatterns.ByHeadsign(
                                routeE,
                                "Heath Street",
                                line,
                                listOf(routePatternE1),
                                listOf(objects.upcomingTrip(schedE1, predE1))
                            ),
                            RealtimePatterns.ByDirection(
                                line,
                                listOf(routeB, routeC, routeE),
                                directionEast,
                                listOf(routePatternB2, routePatternC2, routePatternE2),
                                listOf(
                                    objects.upcomingTrip(schedB2, predB2),
                                    objects.upcomingTrip(schedC2, predC2),
                                    objects.upcomingTrip(schedE2, predE2),
                                )
                            ),
                        ),
                        listOf(directionWest, directionEast)
                    )
                )
            )

        assertEquals(
            listOf(expected),
            staticData.withRealtimeInfo(
                sortByDistanceFrom = stop.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = null,
                filterAtTime = time,
                pinnedRoutes = setOf(),
            )
        )
    }
}
