package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
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
                representativeTrip {
                    id = "test"
                    headsign = "Harvard"
                }
            }
        val route1rp2 =
            objects.routePattern(route1) {
                sortOrder = 1
                representativeTrip {
                    id = "test"
                    headsign = "Harvard"
                }
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
                        station1stop1.id to listOf(route1rp1.id),
                        station1stop2.id to listOf(route1rp2.id),
                        stop2.id to listOf(route1rp3.id),
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
                        headsign(route1, "Boston College", listOf(route1rp1), direction = bDir)
                    }
                    stop(
                        stop3,
                        routes = listOf(route1, route2),
                        directions = listOf(bDir, parkDir)
                    ) {
                        direction(parkDir, listOf(route1, route2), listOf(route1rp2, route2rp2))
                    }
                    stop(stop2, listOf(route2), directions = listOf(cDir, parkDir)) {
                        headsign(route2, "Cleveland Circle", listOf(route2rp1), direction = cDir)
                    }
                }
            },
            NearbyStaticData(global, nearby)
        )
    }

    @Test
    fun `Green Line routes are grouped together without Government Center direction`() {
        val objects = ObjectCollectionBuilder()

        val stopGov = objects.stop { id = "place-gover" }
        // Only get nearby results at Government Center
        val nearby = NearbyResponse(objects)

        // These stops are included because they're thresholds in Direction.specialCases
        val stopArlington = objects.stop { id = "place-armnl" }
        val stopHaymarket = objects.stop { id = "place-haecl" }

        val stop1 = objects.stop { id = "place-lake" }
        val stop2 = objects.stop { id = "place-clmnl" }
        val stop3 = objects.stop { id = "place-river" }
        val stop4 = objects.stop { id = "place-pktrm" }
        val stop5 = objects.stop { id = "place-unsqu" }

        val line = objects.line { id = "line-Green" }

        val routeB =
            objects.route {
                id = "Green-B"
                lineId = line.id
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Boston College", "Government Center")
            }
        val routeC =
            objects.route {
                id = "Green-C"
                lineId = line.id
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Cleveland Circle", "Government Center")
            }
        val routeD =
            objects.route {
                id = "Green-D"
                lineId = line.id
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Riverside", "Union Square")
            }

        val routeBrp1 =
            objects.routePattern(routeB) {
                representativeTrip {
                    headsign = "Boston College"
                    stopIds = listOf(stopGov.id, stop4.id, stopArlington.id, stop1.id)
                }
                directionId = 0
                sortOrder = 3
                typicality = RoutePattern.Typicality.Typical
            }
        val routeBrp2 =
            objects.routePattern(routeB) {
                representativeTrip {
                    headsign = "Government Center"
                    stopIds = listOf(stop1.id, stopArlington.id, stop4.id, stopGov.id)
                }
                directionId = 1
                sortOrder = 4
                typicality = RoutePattern.Typicality.Typical
            }
        val routeCrp1 =
            objects.routePattern(routeC) {
                representativeTrip {
                    headsign = "Cleveland Circle"
                    stopIds = listOf(stopGov.id, stop4.id, stopArlington.id, stop2.id)
                }
                directionId = 0
                sortOrder = 3
                typicality = RoutePattern.Typicality.Typical
            }
        val routeCrp2 =
            objects.routePattern(routeC) {
                representativeTrip {
                    headsign = "Government Center"
                    stopIds = listOf(stop2.id, stopArlington.id, stop4.id, stopGov.id)
                }
                directionId = 1
                sortOrder = 4
                typicality = RoutePattern.Typicality.Typical
            }
        val routeCrp3 =
            objects.routePattern(routeC) {
                representativeTrip {
                    headsign = "Union Square"
                    stopIds =
                        listOf(
                            stop2.id,
                            stopArlington.id,
                            stop4.id,
                            stopGov.id,
                            stopHaymarket.id,
                            stop5.id
                        )
                }
                directionId = 1
                sortOrder = 5
                typicality = RoutePattern.Typicality.Atypical
            }
        val routeDrp1 =
            objects.routePattern(routeD) {
                representativeTrip {
                    headsign = "Riverside"
                    stopIds =
                        listOf(
                            stop5.id,
                            stopHaymarket.id,
                            stopGov.id,
                            stop4.id,
                            stopArlington.id,
                            stop3.id
                        )
                }
                directionId = 0
                sortOrder = 1
                typicality = RoutePattern.Typicality.Typical
            }
        val routeDrp2 =
            objects.routePattern(routeD) {
                representativeTrip {
                    headsign = "Union Square"
                    stopIds =
                        listOf(
                            stop3.id,
                            stopArlington.id,
                            stop4.id,
                            stopGov.id,
                            stopHaymarket.id,
                            stop5.id
                        )
                }

                typicality = RoutePattern.Typicality.Typical
                directionId = 1
                sortOrder = 2
            }

        val global = GlobalResponse(objects)

        val westDir = Direction("West", "Copley & West", 0)
        val northDir = Direction("East", "North Station & North", 1)

        assertEquals(
            NearbyStaticData.build {
                line(line, listOf(routeB, routeC, routeD)) {
                    stop(
                        stopGov,
                        listOf(routeB, routeC, routeD),
                        directions = listOf(westDir, northDir)
                    ) {
                        direction(
                            westDir,
                            listOf(routeB, routeC, routeD),
                            listOf(routeDrp1, routeBrp1, routeCrp1)
                        )
                        direction(northDir, listOf(routeC, routeD), listOf(routeDrp2, routeCrp3))
                    }
                }
            },
            NearbyStaticData(global, nearby)
        )
    }

    @Test
    fun `StopPatterns ForLine groupedDirection helper returns expected Direction objects`() {
        val objects = ObjectCollectionBuilder()
        val route1 =
            objects.route {
                directionNames = listOf("North", "South")
                directionDestinations = listOf("Unique Place", "Shared Place")
            }
        val route2 =
            objects.route {
                directionNames = listOf("North", "South")
                directionDestinations = listOf("Other Unique Place", "Shared Place")
            }
        val line = objects.line()
        val headsignPattern1Route1 =
            objects.routePattern(route1) {
                typicality = RoutePattern.Typicality.Typical
                directionId = 0
            }
        val headsignPattern2Route1 =
            objects.routePattern(route1) {
                typicality = RoutePattern.Typicality.Typical
                directionId = 1
            }
        val headsignPattern3Route1 =
            objects.routePattern(route1) {
                typicality = RoutePattern.Typicality.Atypical
                directionId = 0
            }
        val headsignPattern1Route2 =
            objects.routePattern(route2) {
                typicality = RoutePattern.Typicality.Typical
                directionId = 0
            }
        val headsignPattern2Route2 =
            objects.routePattern(route2) {
                typicality = RoutePattern.Typicality.Typical
                directionId = 1
            }
        val staticByHeadsign1 =
            NearbyStaticData.StaticPatterns.ByHeadsign(
                route1,
                "Unique Place",
                line,
                listOf(headsignPattern1Route1),
                emptySet(),
            )
        val staticByHeadsign1Atypical =
            NearbyStaticData.StaticPatterns.ByHeadsign(
                route1,
                "Third Unique Place",
                line,
                listOf(headsignPattern3Route1),
                emptySet(),
            )
        val staticByHeadsign2 =
            NearbyStaticData.StaticPatterns.ByHeadsign(
                route2,
                "Other Unique Place",
                line,
                listOf(headsignPattern1Route2),
                emptySet(),
            )
        val overriddenDirection = Direction("South", "Overridden Value", 0)
        val staticByHeadsign2WithDirection =
            NearbyStaticData.StaticPatterns.ByHeadsign(
                route2,
                "Other Unique Place",
                line,
                listOf(headsignPattern1Route2),
                emptySet(),
                overriddenDirection
            )
        val sharedDirection = Direction("South", "Shared Place", 1)
        val staticByDirection =
            NearbyStaticData.StaticPatterns.ByDirection(
                line,
                listOf(route1, route2),
                sharedDirection,
                listOf(headsignPattern2Route1, headsignPattern2Route2),
                emptySet()
            )

        assertEquals(
            Direction("North", null, 0),
            NearbyStaticData.StopPatterns.ForLine.groupedDirection(
                listOf(staticByHeadsign1, staticByHeadsign2, staticByDirection),
                listOf(route1, route2),
                0
            )
        )
        assertEquals(
            Direction("North", "Other Unique Place", 0),
            NearbyStaticData.StopPatterns.ForLine.groupedDirection(
                listOf(staticByHeadsign1Atypical, staticByHeadsign2, staticByDirection),
                listOf(route1, route2),
                0
            )
        )
        assertEquals(
            Direction("North", "Unique Place", 0),
            NearbyStaticData.StopPatterns.ForLine.groupedDirection(
                listOf(staticByHeadsign1Atypical, staticByDirection),
                listOf(route1, route2),
                0
            )
        )
        assertEquals(
            overriddenDirection,
            NearbyStaticData.StopPatterns.ForLine.groupedDirection(
                listOf(staticByHeadsign2WithDirection, staticByDirection),
                listOf(route1, route2),
                0
            )
        )
        assertEquals(
            sharedDirection,
            NearbyStaticData.StopPatterns.ForLine.groupedDirection(
                listOf(staticByHeadsign1, staticByHeadsign2, staticByDirection),
                listOf(route1, route2),
                1
            )
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
                                    ),
                                    allDataLoaded = false
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
                                    listOf(objects.upcomingTrip(stop2Pattern3Prediction)),
                                    allDataLoaded = false
                                )
                            )
                        )
                    )
                ),
            ),
            staticData.withRealtimeInfo(
                globalData = GlobalResponse(objects),
                sortByDistanceFrom = stop1.position,
                schedules = null,
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
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
                                    listOf(objects.upcomingTrip(typicalOutboundPrediction)),
                                    allDataLoaded = false
                                ),
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "Deviation Out",
                                    null,
                                    listOf(deviationOutbound),
                                    listOf(objects.upcomingTrip(deviationOutboundPrediction)),
                                    allDataLoaded = false
                                ),
                                // since this has trips it's sorted earlier than typical in
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "Atypical In",
                                    null,
                                    listOf(atypicalInbound),
                                    listOf(objects.upcomingTrip(atypicalInboundPrediction)),
                                    allDataLoaded = false
                                ),
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "Typical In",
                                    null,
                                    listOf(typicalInbound),
                                    emptyList(),
                                    allDataLoaded = false
                                ),
                            )
                        )
                    )
                )
            ),
            staticData.withRealtimeInfo(
                globalData = GlobalResponse(objects),
                sortByDistanceFrom = stop1.position,
                schedules = null,
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(objects),
                filterAtTime = time,
                pinnedRoutes = setOf(),
            )
        )
    }

    @Test
    fun `withRealtimeInfo handles schedule and predictions edge cases`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()

        val route1 = objects.route()

        // exclude, schedule in past
        val schedulePast =
            objects.routePattern(route1) {
                sortOrder = 1
                representativeTrip { headsign = "Schedule Past" }
            }
        // include, schedule upcoming
        val scheduleSoon =
            objects.routePattern(route1) {
                sortOrder = 2
                representativeTrip { headsign = "Schedule Soon" }
            }
        // exclude, schedule too late
        val scheduleLater =
            objects.routePattern(route1) {
                sortOrder = 3
                representativeTrip { headsign = "Schedule Later" }
            }
        // exclude, prediction in past
        val predictionPast =
            objects.routePattern(route1) {
                sortOrder = 4
                representativeTrip { headsign = "Prediction Past" }
            }
        // include, prediction in past but BRD
        val predictionBrd =
            objects.routePattern(route1) {
                sortOrder = 5
                representativeTrip { headsign = "Prediction BRD" }
            }
        // include, prediction upcoming
        val predictionSoon =
            objects.routePattern(route1) {
                sortOrder = 6
                representativeTrip { headsign = "Prediction Soon" }
            }
        // exclude, prediction later
        val predictionLater =
            objects.routePattern(route1) {
                sortOrder = 7
                representativeTrip { headsign = "Prediction Later" }
            }

        val staticData =
            NearbyStaticData.build {
                route(route1) {
                    stop(stop1) {
                        headsign("Schedule Past", listOf(schedulePast))
                        headsign("Schedule Soon", listOf(scheduleSoon))
                        headsign("Schedule Later", listOf(scheduleLater))
                        headsign("Prediction Past", listOf(predictionPast))
                        headsign("Prediction BRD", listOf(predictionBrd))
                        headsign("Prediction Soon", listOf(predictionSoon))
                        headsign("Prediction Later", listOf(predictionLater))
                    }
                }
            }

        val time = Instant.parse("2024-09-19T13:43:19-04:00")

        val schedulePastSchedule =
            objects.schedule {
                stopId = stop1.id
                trip = objects.trip(schedulePast)
                departureTime = time - 1.minutes
            }
        val scheduleSoonSchedule =
            objects.schedule {
                stopId = stop1.id
                trip = objects.trip(scheduleSoon)
                departureTime = time + 5.minutes
            }
        val scheduleLaterSchedule =
            objects.schedule {
                stopId = stop1.id
                trip = objects.trip(scheduleLater)
                departureTime = time + 91.minutes
            }
        val predictionPastPrediction =
            objects.prediction {
                stopId = stop1.id
                trip = objects.trip(predictionPast)
                departureTime = time - 1.minutes
            }
        val predictionBrdTrip = objects.trip(predictionBrd)
        val predictionBrdVehicle =
            objects.vehicle {
                stopId = stop1.id
                tripId = predictionBrdTrip.id
                currentStatus = Vehicle.CurrentStatus.StoppedAt
            }
        val predictionBrdPrediction =
            objects.prediction {
                stopId = stop1.id
                trip = predictionBrdTrip
                departureTime = time - 1.minutes
                vehicleId = predictionBrdVehicle.id
            }
        val predictionSoonPrediction =
            objects.prediction {
                stopId = stop1.id
                trip = objects.trip(predictionSoon)
                departureTime = time + 5.minutes
            }
        val predictionLaterPrediction =
            objects.prediction {
                stopId = stop1.id
                trip = objects.trip(predictionLater)
                departureTime = time + 91.minutes
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
                                    "Schedule Soon",
                                    null,
                                    listOf(scheduleSoon),
                                    listOf(objects.upcomingTrip(scheduleSoonSchedule))
                                ),
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "Prediction BRD",
                                    null,
                                    listOf(predictionBrd),
                                    listOf(
                                        objects.upcomingTrip(
                                            predictionBrdPrediction,
                                            predictionBrdVehicle
                                        )
                                    ),
                                    hasSchedulesToday = false
                                ),
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "Prediction Soon",
                                    null,
                                    listOf(predictionSoon),
                                    listOf(objects.upcomingTrip(predictionSoonPrediction)),
                                    hasSchedulesToday = false
                                )
                            )
                        )
                    )
                )
            ),
            staticData.withRealtimeInfo(
                globalData = GlobalResponse(objects),
                sortByDistanceFrom = stop1.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
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
                                    upcomingTrips = emptyList(),
                                    allDataLoaded = false
                                ),
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "Typical In",
                                    null,
                                    listOf(typicalInbound),
                                    upcomingTrips = emptyList(),
                                    allDataLoaded = false
                                ),
                            )
                        )
                    )
                )
            ),
            staticData.withRealtimeInfo(
                globalData = GlobalResponse(objects),
                sortByDistanceFrom = stop1.position,
                schedules = null,
                predictions = PredictionsStreamDataResponse(emptyMap(), emptyMap(), emptyMap()),
                alerts = AlertsStreamDataResponse(emptyMap()),
                filterAtTime = time,
                pinnedRoutes = setOf(),
            )
        )
    }

    @Test
    fun `withRealtimeInfo hideNonTypicalPatternsBeyondNext when null doesn't filter`() {
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

        // should be included because hideNonTypicalPatternsBeyondNext is null
        val deviationInbound =
            objects.routePattern(route1) {
                directionId = 1
                sortOrder = 4
                typicality = RoutePattern.Typicality.Deviation
                representativeTrip { headsign = "Deviation In" }
            }

        val staticData =
            NearbyStaticData.build {
                route(route1) {
                    stop(stop1) {
                        headsign("Typical Out", listOf(typicalOutbound))
                        headsign("Deviation In", listOf(deviationInbound))
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

        val deviationInboundPrediction =
            objects.prediction {
                departureTime = time + 95.minutes
                routeId = route1.id
                stopId = stop1.id
                tripId = deviationInbound.representativeTripId
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
                                    listOf(objects.upcomingTrip(typicalOutboundPrediction)),
                                    allDataLoaded = false
                                ),
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "Deviation In",
                                    null,
                                    listOf(deviationInbound),
                                    listOf(objects.upcomingTrip(deviationInboundPrediction)),
                                    allDataLoaded = false
                                ),
                            )
                        )
                    )
                )
            ),
            staticData.withRealtimeInfo(
                globalData = GlobalResponse(objects),
                sortByDistanceFrom = stop1.position,
                schedules = null,
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
                filterAtTime = time,
                showAllPatternsWhileLoading = false,
                hideNonTypicalPatternsBeyondNext = null,
                filterCancellations = false,
                pinnedRoutes = setOf(),
            )
        )
    }

    @Test
    fun `withRealtimeInfo includes cancellations when filterCancellations false`() {
        val objects = ObjectCollectionBuilder()
        val stop1 = objects.stop()
        val route1 = objects.route()

        // should be included because typical and has cancelled prediction
        val typicalOutbound =
            objects.routePattern(route1) {
                directionId = 0
                sortOrder = 1
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Typical Out" }
            }

        val staticData =
            NearbyStaticData.build {
                route(route1) { stop(stop1) { headsign("Typical Out", listOf(typicalOutbound)) } }
            }

        val time = Instant.parse("2024-02-22T12:08:19-05:00")

        val typicalOutboundSchedule =
            objects.schedule {
                routeId = route1.id
                tripId = typicalOutbound.representativeTripId
                stopId = stop1.id
                arrivalTime = time
                departureTime = time
            }

        val typicalOutboundPrediction =
            objects.prediction {
                departureTime = null
                routeId = route1.id
                stopId = stop1.id
                tripId = typicalOutbound.representativeTripId
                scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
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
                                    listOf(
                                        objects.upcomingTrip(
                                            typicalOutboundSchedule,
                                            typicalOutboundPrediction
                                        )
                                    ),
                                    allDataLoaded = true
                                ),
                            )
                        )
                    )
                )
            ),
            staticData.withRealtimeInfo(
                globalData = GlobalResponse(objects),
                sortByDistanceFrom = stop1.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
                filterAtTime = time,
                showAllPatternsWhileLoading = false,
                hideNonTypicalPatternsBeyondNext = null,
                filterCancellations = false,
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
                globalData = GlobalResponse(objects),
                sortByDistanceFrom = closeBusStop.position,
                predictions = PredictionsStreamDataResponse(objects),
                schedules = ScheduleResponse(objects),
                alerts = AlertsStreamDataResponse(objects),
                filterAtTime = time,
                pinnedRoutes = setOf(),
            )
        assertEquals(
            listOf(closeSubwayRoute, farSubwayRoute, closeBusRoute, farBusRoute),
            checkNotNull(realtimeRoutesSorted).flatMap {
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
                globalData = GlobalResponse(objects),
                sortByDistanceFrom = closeBusStop.position,
                predictions = PredictionsStreamDataResponse(objects),
                schedules = ScheduleResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
                filterAtTime = time,
                pinnedRoutes = setOf(farBusRoute.id, farSubwayRoute.id),
            )
        assertEquals(
            listOf(farSubwayRoute, farBusRoute, closeSubwayRoute, closeBusRoute),
            checkNotNull(realtimeRoutesSorted).flatMap {
                when (it) {
                    is StopsAssociated.WithRoute -> listOf(it.route)
                    is StopsAssociated.WithLine -> it.routes
                }
            }
        )
    }

    @Test
    fun `withRealtimeInfo sorts routes with no service today to the bottom`() {
        val objects = ObjectCollectionBuilder()

        val closeBusStop = objects.stop()
        val midBusStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.2
                longitude = closeBusStop.longitude + 0.2
            }
        val farBusStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.4
                longitude = closeBusStop.longitude + 0.4
            }
        val closeSubwayStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.1
                longitude = closeBusStop.longitude + 0.1
            }
        val midSubwayStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.3
                longitude = closeBusStop.longitude + 0.3
            }
        val farSubwayStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.5
                longitude = closeBusStop.longitude + 0.5
            }

        // Unpinned, no schedules
        val closeBusRoute =
            objects.route {
                id = "close-bus"
                type = RouteType.BUS
            }
        // Unpinned, with schedules
        val midBusRoute =
            objects.route {
                id = "mid-bus"
                type = RouteType.BUS
            }
        // Unpinned, no schedules
        val farBusRoute =
            objects.route {
                id = "far-bus"
                type = RouteType.BUS
            }
        // Unpinned, no schedules
        val closeSubwayRoute =
            objects.route {
                id = "close-subway"
                type = RouteType.HEAVY_RAIL
            }
        // Pinned, no schedules
        val midSubwayRoute =
            objects.route {
                id = "mid-subway"
                type = RouteType.LIGHT_RAIL
            }
        // Pinned, with schedules
        val farSubwayRoute =
            objects.route {
                id = "far-subway"
                type = RouteType.HEAVY_RAIL
            }

        val closeBusPattern =
            objects.routePattern(closeBusRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Lincoln Lab" }
                typicality = RoutePattern.Typicality.Typical
            }
        val midBusPattern1 =
            objects.routePattern(midBusRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Nubian" }
                typicality = RoutePattern.Typicality.Typical
            }
        val midBusPattern2 =
            objects.routePattern(midBusRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Nubian" }
                typicality = RoutePattern.Typicality.Typical
            }
        val farBusPattern1 =
            objects.routePattern(farBusRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Malden Center" }
                typicality = RoutePattern.Typicality.Typical
            }
        val farBusPattern2 =
            objects.routePattern(farBusRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Malden Center" }
                typicality = RoutePattern.Typicality.Atypical
            }
        val closeSubwayPattern =
            objects.routePattern(closeSubwayRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Alewife" }
                typicality = RoutePattern.Typicality.Typical
            }
        val midSubwayPattern =
            objects.routePattern(midSubwayRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Medford/Tufts" }
                typicality = RoutePattern.Typicality.Typical
            }
        val farSubwayPattern =
            objects.routePattern(farSubwayRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Oak Grove" }
                typicality = RoutePattern.Typicality.Typical
            }

        val staticData =
            NearbyStaticData.build {
                route(farBusRoute) {
                    stop(farBusStop) {
                        headsign("Malden Center", listOf(farBusPattern1, farBusPattern2))
                    }
                }
                route(midBusRoute) {
                    stop(midBusStop) { headsign("Nubian", listOf(midBusPattern1, midBusPattern2)) }
                }
                route(closeBusRoute) {
                    stop(closeBusStop) { headsign("Lincoln Lab", listOf(closeBusPattern)) }
                }
                route(farSubwayRoute) {
                    stop(farSubwayStop) { headsign("Oak Grove", listOf(farSubwayPattern)) }
                }
                route(midSubwayRoute) {
                    stop(midSubwayStop) { headsign("Medford/Tufts", listOf(midSubwayPattern)) }
                }
                route(closeSubwayRoute) {
                    stop(closeSubwayStop) { headsign("Alewife", listOf(closeSubwayPattern)) }
                }
            }

        val time = Instant.parse("2024-02-21T09:30:08-05:00")

        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = midBusRoute.id
            stopId = midBusStop.id
            tripId = midBusPattern1.representativeTripId
        }

        objects.schedule {
            routeId = midBusRoute.id
            tripId = midBusPattern1.representativeTripId
        }

        objects.schedule {
            routeId = farSubwayRoute.id
            tripId = farSubwayPattern.representativeTripId
        }

        val realtimeRoutesSorted =
            staticData.withRealtimeInfo(
                globalData = GlobalResponse(objects),
                sortByDistanceFrom = closeBusStop.position,
                predictions = PredictionsStreamDataResponse(objects),
                schedules = ScheduleResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
                filterAtTime = time,
                pinnedRoutes = setOf(midSubwayRoute.id, farSubwayRoute.id),
            )

        // Routes with no service today should sort below all routes with any service today,
        // unless they are a pinned route, in which case we want them to sort beneath all other
        // pinned routes, but above any unpinned ones. Here, the far and mid subway routes are both
        // pinned, but mid has no scheduled service, so it's sorted below the farther pinned route.
        // For unpinned routes, mid bus is the only one with any schedules, so it's sorted above all
        // the other unpinned routes, then the remaining  are ordered with the usual nearby transit
        // sort order, subway first, then by distance.
        assertEquals(
            listOf(
                farSubwayRoute,
                midSubwayRoute,
                midBusRoute,
                closeSubwayRoute,
                closeBusRoute,
                farBusRoute
            ),
            checkNotNull(realtimeRoutesSorted).flatMap {
                when (it) {
                    is StopsAssociated.WithRoute -> listOf(it.route)
                    is StopsAssociated.WithLine -> it.routes
                }
            }
        )
    }

    @Test
    fun `withRealtimeInfo doesn't sort unscheduled routes to the bottom if they are disrupted`() {
        val objects = ObjectCollectionBuilder()

        val closeSubwayStop = objects.stop()
        val midSubwayStop =
            objects.stop {
                latitude = closeSubwayStop.latitude + 0.3
                longitude = closeSubwayStop.longitude + 0.3
            }
        val farSubwayStop =
            objects.stop {
                latitude = closeSubwayStop.latitude + 0.5
                longitude = closeSubwayStop.longitude + 0.5
            }

        // No alerts, no schedules
        val closeSubwayRoute =
            objects.route {
                id = "close-subway"
                type = RouteType.HEAVY_RAIL
            }
        // Some alerts, no schedules
        val midSubwayRoute =
            objects.route {
                id = "mid-subway"
                type = RouteType.LIGHT_RAIL
            }
        // No alerts, some schedules
        val farSubwayRoute =
            objects.route {
                id = "far-subway"
                type = RouteType.HEAVY_RAIL
            }

        val closeSubwayPattern =
            objects.routePattern(closeSubwayRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Alewife" }
                typicality = RoutePattern.Typicality.Typical
            }
        val midSubwayPattern =
            objects.routePattern(midSubwayRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Medford/Tufts" }
                typicality = RoutePattern.Typicality.Typical
            }
        val farSubwayPattern =
            objects.routePattern(farSubwayRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Oak Grove" }
                typicality = RoutePattern.Typicality.Typical
            }

        val staticData =
            NearbyStaticData.build {
                route(farSubwayRoute) {
                    stop(farSubwayStop) { headsign("Oak Grove", listOf(farSubwayPattern)) }
                }
                route(midSubwayRoute) {
                    stop(midSubwayStop) { headsign("Medford/Tufts", listOf(midSubwayPattern)) }
                }
                route(closeSubwayRoute) {
                    stop(closeSubwayStop) { headsign("Alewife", listOf(closeSubwayPattern)) }
                }
            }

        val time = Instant.parse("2024-02-21T09:30:08-05:00")

        objects.schedule {
            routeId = farSubwayRoute.id
            tripId = farSubwayPattern.representativeTripId
        }

        objects.alert {
            activePeriod(time.minus(2.days), time.plus(2.days))
            effect = Alert.Effect.Suspension
            informedEntity(
                listOf(
                    Alert.InformedEntity.Activity.Board,
                    Alert.InformedEntity.Activity.Exit,
                    Alert.InformedEntity.Activity.Ride
                ),
                route = midSubwayRoute.id,
                routeType = midSubwayRoute.type,
                stop = midSubwayStop.id
            )
        }

        val realtimeRoutesSorted =
            staticData.withRealtimeInfo(
                globalData = GlobalResponse(objects),
                sortByDistanceFrom = closeSubwayStop.position,
                predictions = PredictionsStreamDataResponse(objects),
                schedules = ScheduleResponse(objects),
                alerts = AlertsStreamDataResponse(objects),
                filterAtTime = time,
                pinnedRoutes = setOf(),
            )

        // If a route has major disruptions and doesn't have any scheduled trips, it should still
        // be sorted as it normally would be.
        assertEquals(
            listOf(
                midSubwayRoute,
                farSubwayRoute,
                closeSubwayRoute,
            ),
            checkNotNull(realtimeRoutesSorted).flatMap {
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
                                    listOf(objects.upcomingTrip(prediction1)),
                                    allDataLoaded = false
                                )
                            )
                        )
                    )
                )
            ),
            staticData.withRealtimeInfo(
                globalData = GlobalResponse(objects),
                sortByDistanceFrom = parentStop.position,
                schedules = null,
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(objects),
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
                globalData = GlobalResponse(objects),
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
    fun `withRealtimeInfo checks if any trips are scheduled all day`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route()
        val routePatternA =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "A" }
            }
        val routePatternB =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "B" }
            }
        val routePatternC =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Deviation
                representativeTrip { headsign = "C" }
            }
        val trip1 = objects.trip(routePatternA)

        val time = Instant.parse("2024-03-14T12:23:44-04:00")

        objects.schedule {
            trip = trip1
            stopId = stop.id
            stopSequence = 90
            departureTime = time - 2.hours
        }

        val staticData =
            NearbyStaticData.build {
                route(route) {
                    stop(stop) {
                        headsign("A", listOf(routePatternA))
                        headsign("B", listOf(routePatternB))
                        headsign("C", listOf(routePatternC))
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
                                    listOf(routePatternA),
                                    emptyList()
                                ),
                                RealtimePatterns.ByHeadsign(
                                    route,
                                    "B",
                                    null,
                                    listOf(routePatternB),
                                    emptyList(),
                                    hasSchedulesToday = false
                                )
                            )
                        )
                    )
                )
            ),
            staticData.withRealtimeInfo(
                globalData = GlobalResponse(objects),
                sortByDistanceFrom = stop.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
                filterAtTime = time,
                pinnedRoutes = setOf(),
            )
        )
    }

    @Test
    fun `withRealtimeInfo checks route along with route pattern and stop`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route1 = objects.route { sortOrder = 1 }
        val routePattern1 = objects.routePattern(route1)
        val trip1 = objects.trip(routePattern1)

        val route2 = objects.route { sortOrder = 2 }
        val routePattern2 = objects.routePattern(route2)
        val trip2 = objects.trip(routePattern2)

        // Should not be included
        val trip3 = objects.trip(routePattern2) { routePatternId = "not the right id" }

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
        val sched3 =
            objects.schedule {
                trip = trip3
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 3.minutes
            }

        val pred1 = objects.prediction(sched1) { departureTime = time + 1.5.minutes }
        val pred2 = objects.prediction(sched2) { departureTime = time + 2.3.minutes }
        val pred3 = objects.prediction(sched3) { departureTime = time + 3.4.minutes }

        val staticData =
            NearbyStaticData.build {
                route(route1) { stop(stop) { headsign("A", listOf(routePattern1)) } }
                route(route2) { stop(stop) { headsign("B", listOf(routePattern2)) } }
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
                                    "B",
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
                globalData = GlobalResponse(objects),
                sortByDistanceFrom = stop.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
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
        objects.schedule {
            this.trip = objects.trip(routePattern)
            stopId = stop.id
            departureTime = time.minus(1.hours)
        }

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
                                    alertsHere = listOf(alert),
                                    alertsDownstream = listOf()
                                )
                            )
                        )
                    )
                )
            ),
            staticData.withRealtimeInfo(
                globalData = GlobalResponse(objects),
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
                globalData = GlobalResponse(objects),
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
                id = "test-hs"
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
                globalData = GlobalResponse(objects),
                sortByDistanceFrom = stop.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
                filterAtTime = time,
                pinnedRoutes = setOf(),
            )
        )
    }

    @Test
    fun `NearbyStaticData getSchedulesTodayByPattern determines which patterns have service today`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route()
        val routePatternA =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "A" }
            }
        val routePatternB =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "B" }
            }
        val trip1 = objects.trip(routePatternA)

        val time = Instant.parse("2024-03-14T12:23:44-04:00")

        objects.schedule {
            trip = trip1
            stopId = stop.id
            stopSequence = 90
            departureTime = time - 2.hours
        }

        assertNull(NearbyStaticData.getSchedulesTodayByPattern(null))

        val hasSchedulesToday =
            NearbyStaticData.getSchedulesTodayByPattern(ScheduleResponse(objects))

        assertTrue(hasSchedulesToday?.get(routePatternA.id)!!)
        assertNull(hasSchedulesToday[routePatternB.id])
    }
}
