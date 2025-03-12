package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.parametric.parametricTest
import kotlinx.datetime.Instant
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RouteCardDataTest {

    @Test
    fun `ListBuilder addStaticStopsData when there are no new patterns for a stop then it is omitted`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()
        val stop2 = objects.stop()

        val route1 = objects.route()

        val route1rp1 = objects.routePattern(route1) {
            representativeTrip {
                headsign = "Harvard"
                directionId = 0
            }
        }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                mapOf(
                    stop1.id to listOf(route1rp1.id),
                    stop2.id to listOf(route1rp1.id),
                ),
            )
        val nearby = NearbyResponse(objects)

        assertEquals(
            mapOf(
                route1.id to
                        RouteCardData.Builder(
                            RouteCardData.LineOrRoute.Route(route1), mapOf(
                                stop1.id to RouteCardData.RouteStopDataBuilder(
                                    stop1, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
                                            directionId = 0,
                                            routePatterns = listOf(route1rp1),
                                            stopIds = setOf(stop1.id)
                                        )
                                    )
                                ),


                                )
                        )
            ),
            RouteCardData.ListBuilder().addStaticStopsData(nearby.stopIds, global).data
        )
    }

    @Test
    fun `ListBuilder addStaticStopsData when second stop serves a new rp for route served by previous stop then include all rps at second stop`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()
        val stop2 = objects.stop()

        val route1 = objects.route()

        val route1rp1 = objects.routePattern(route1) {
            representativeTrip {
                headsign = "Harvard"
                directionId = 0
            }
        }
        val route1rp2 = objects.routePattern(route1) {
            representativeTrip {
                headsign = "Nubian"
                directionId = 0
            }
        }

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
            mapOf(
                route1.id to
                        RouteCardData.Builder(
                            RouteCardData.LineOrRoute.Route(route1), mapOf(
                                stop1.id to RouteCardData.RouteStopDataBuilder(
                                    stop1, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
                                            directionId = 0,
                                            routePatterns = listOf(route1rp1),
                                            stopIds = setOf(stop1.id)
                                        )
                                    )
                                ),
                                stop2.id to RouteCardData.RouteStopDataBuilder(
                                    stop2, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
                                            directionId = 0,
                                            routePatterns = listOf(
                                                route1rp1,
                                                route1rp2
                                            )
                                            ,
                                            stopIds = setOf(stop2.id)
                                        )
                                    )
                                ),


                                )
                        )
            ),
            RouteCardData.ListBuilder().addStaticStopsData(nearby.stopIds, global).data
        )
    }

    @Test
    fun `ListBuilder addStaticStopsData groups patterns by direction`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()

        val route1 = objects.route()

        val route1rp1 = objects.routePattern(route1) {
            representativeTrip { headsign = "Harvard" }
            directionId = 0
        }
        val route1rp2 = objects.routePattern(route1) {
            representativeTrip { headsign = "Harvard v2" }
            directionId = 0
        }
        val route1rp3 = objects.routePattern(route1) {
            representativeTrip { headsign = "Nubian" }
            directionId = 1
        }


        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                mapOf(
                    stop1.id to listOf(route1rp1.id, route1rp2.id, route1rp3.id),
                ),
            )
        val nearby = NearbyResponse(objects)

        assertEquals(
            mapOf(
                route1.id to
                        RouteCardData.Builder(
                            RouteCardData.LineOrRoute.Route(route1), mapOf(
                                stop1.id to RouteCardData.RouteStopDataBuilder(
                                    stop1, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
                                            directionId = 0,
                                            routePatterns = listOf(
                                                route1rp1,
                                                route1rp2
                                            )
                                            ,
                                            stopIds = setOf(stop1.id)
                                        ),
                                        1 to RouteCardData.LeafBuilder(
                                            directionId = 1,
                                            routePatterns = listOf(
                                                route1rp3
                                            ),
                                            stopIds = setOf(stop1.id)
                                        )

                                    )
                                ),


                                )
                        )
            ),
            RouteCardData.ListBuilder().addStaticStopsData(nearby.stopIds, global).data
        )
    }

    @Test
    fun `ListBuilder addStaticStopsData when a stop is served by multiple routes it is included for each route`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()

        val route1 = objects.route()
        val route2 = objects.route()


        val route1rp1 = objects.routePattern(route1) {
            representativeTrip { headsign = "Harvard" }
            directionId = 0
        }
        val route2rp1 = objects.routePattern(route2) {
            representativeTrip { headsign = "Kenmore" }
            directionId = 0
        }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                mapOf(
                    stop1.id to listOf(route1rp1.id, route2rp1.id)
                ),
            )
        val nearby = NearbyResponse(objects)

        assertEquals(
            mapOf(
                route1.id to
                        RouteCardData.Builder(
                            RouteCardData.LineOrRoute.Route(route1), mapOf(
                                stop1.id to RouteCardData.RouteStopDataBuilder(
                                    stop1, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
                                            directionId = 0,
                                            routePatterns = listOf(route1rp1),
                                            stopIds = setOf(stop1.id)
                                        )

                                    )
                                ),
                                )
                        ),
                route2.id to
                        RouteCardData.Builder(
                            RouteCardData.LineOrRoute.Route(route2), mapOf(
                                stop1.id to RouteCardData.RouteStopDataBuilder(
                                    stop1, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
                                            directionId = 0,
                                            routePatterns = listOf(route2rp1),
                                            stopIds = setOf(stop1.id)
                                        )

                                    )
                                ),
                            )
                        )
            ),
            RouteCardData.ListBuilder().addStaticStopsData(nearby.stopIds, global).data
        )
    }

    @Test
    fun `ListBuilder addStaticStopsData groups by parent station`() {
        val objects = ObjectCollectionBuilder()

        val station1 = objects.stop {
            id = "station_1"
        }

        val station1stop1 = objects.stop {
            parentStationId = station1.id
            id = "stop_1" }
        val station1stop2 = objects.stop {
            parentStationId = station1.id
            id = "stop_2" }

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
            mapOf(
                route1.id to
                        RouteCardData.Builder(
                            RouteCardData.LineOrRoute.Route(route1), mapOf(
                                station1.id to RouteCardData.RouteStopDataBuilder(
                                    station1, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
                                            directionId = 0,
                                            routePatterns = listOf(
                                                route1rp1, route1rp2
                                            ),
                                            stopIds = setOf(station1.id, station1stop1.id, station1stop2.id)

                                        )
                                    )
                                ),
                                stop2.id to RouteCardData.RouteStopDataBuilder(
                                    stop2, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
                                            directionId = 0,
                                            routePatterns = listOf(
                                                route1rp3
                                            ),
                                            stopIds = setOf(stop2.id)
                                        )
                                    )
                                ),
                            )
                        ),
            ),
            RouteCardData.ListBuilder().addStaticStopsData(nearby.stopIds, global).data
        )
    }

    @Test
    fun `ListBuilder addStaticStopsData preserves unscheduled physical platform`() {
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
            mapOf(
                route.id to
                        RouteCardData.Builder(
                            RouteCardData.LineOrRoute.Route(route), mapOf(
                                parentStation.id to RouteCardData.RouteStopDataBuilder(
                                    parentStation, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
                                            directionId = 0,
                                            routePatterns = listOf(routePattern ),
                                            stopIds = setOf(parentStation.id, logicalPlatform.id, physicalPlatform.id)
                                        )
                                    )
                                ),
                            )
                        ),
            ),
            RouteCardData.ListBuilder().addStaticStopsData(nearby.stopIds, global).data
        )


    }


    @Test
    fun `ListBuilder addStaticStopsData Green Line shuttles are not grouped together`() {
        val objects = ObjectCollectionBuilder()

        val stop = objects.stop()

        val line =
            objects.line {
                id = "line-Green"
                sortOrder = 0
            }

        val railRoute =
            objects.route {
                lineId = line.id
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Boston College", "Government Center")
            }
        val shuttleRoute =
            objects.route {
                id = "Shuttle-$id"
                lineId = line.id
            }

        val railPattern = objects.routePattern(railRoute) {
            representativeTrip { headsign = "Boston College" }
        }
        val shuttlePattern = objects.routePattern(shuttleRoute) {
            representativeTrip { headsign = "Boston College" }
        }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop = mapOf(stop.id to listOf(railPattern.id, shuttlePattern.id)),
            )
        val nearby = NearbyResponse(objects)

        // TODO: Actually test for these directions
        val westDir = Direction("West", "Boston College", 0)
        val eastDir = Direction("East", "Government Center", 1)

        assertEquals(
            mapOf(
                line.id to
                        RouteCardData.Builder(
                            RouteCardData.LineOrRoute.Line(line, setOf(railRoute)), mapOf(
                                stop.id to RouteCardData.RouteStopDataBuilder(
                                    stop, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
                                            directionId = 0,
                                            routePatterns = listOf(railPattern ),
                                            stopIds = setOf(stop.id)
                                        )
                                    )
                                ),
                            )
                        ),
                shuttleRoute.id to
                RouteCardData.Builder(RouteCardData.LineOrRoute.Route(shuttleRoute), mapOf(
                    stop.id to RouteCardData.RouteStopDataBuilder(stop, emptyList(), mapOf(
                        0 to RouteCardData.LeafBuilder(
                            directionId = 0,
                            routePatterns = listOf(shuttlePattern ),
                            stopIds = setOf(stop.id)
                        )
                    ))
                ))
            ),
            RouteCardData.ListBuilder().addStaticStopsData(nearby.stopIds, global).data
        )



    }

    @Test
    @Ignore // I expect this will pass once we have filtering in place, but I'm not entirely sure.
    // May be something else missing in the direction logic
    fun `ListBuilder addStaticStopsData Green Line routes are grouped together without Government Center direction`() {
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
                id= "routeBrp1"
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
                id= "routeBrp2"
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
                id= "routeCrp1"
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
                id= "routeCrp2"
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
                id= "routeCrp3"
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
                id= "routeDrp1"
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
                id= "routeDrp2"
                typicality = RoutePattern.Typicality.Typical
                directionId = 1
                sortOrder = 2
            }

        val global = GlobalResponse(objects)

        // TODO: Actually test for these directions
        val westDir = Direction("West", "Copley & West", 0)
        val northDir = Direction("East", "North Station & North", 1)

        assertEquals(
            mapOf(
                line.id to
                        RouteCardData.Builder(
                            RouteCardData.LineOrRoute.Line(line, setOf(routeB, routeC, routeD)), mapOf(
                                stopGov.id to RouteCardData.RouteStopDataBuilder(
                                    stopGov, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
                                            directionId = 0,
                                            routePatterns = listOf(routeDrp1, routeBrp1, routeCrp1 ),
                                            stopIds = setOf(stopGov.id)
                                        ),
                                        1 to RouteCardData.LeafBuilder(
                                            directionId = 1,
                                            routePatterns = listOf(routeDrp2, routeCrp3 ),
                                            stopIds = setOf(stopGov.id)
                                        )
                                    )
                                ),
                            )
                        )
            ),
            RouteCardData.ListBuilder()
                .addStaticStopsData(nearby.stopIds, global)
                .data
        )


    }

    @Test
    fun `ListBuilder addUpcomingTrips includes predictions filtered to the correct stop and direction`() {
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

        val global = GlobalResponse(objects, patternIdsByStop = mapOf(stop1.id to listOf(pattern1.id, pattern2.id),
            stop2.id to listOf(pattern3.id)) )

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

            // should *not* be ignored since pattern 1 shows at stop, but pattern 3 does not
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
            mapOf(
                route1.id to
                        RouteCardData.Builder(
                            RouteCardData.LineOrRoute.Route(route1), mapOf(
                                stop1.id to RouteCardData.RouteStopDataBuilder(
                                    stop1, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
                                            directionId = 0,
                                            routePatterns = listOf(pattern1, pattern2),
                                            stopIds = setOf(stop1.id),
                                            upcomingTrips = listOf(
                                                objects.upcomingTrip(stop1Pattern2Prediction),
                                                objects.upcomingTrip(stop1Pattern1Prediction)
                                            ),
                                            allDataLoaded = false
                                        )
                                    )
                                ),
                                stop2.id to RouteCardData.RouteStopDataBuilder(
                                    stop2, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
                                            directionId = 0,
                                            routePatterns = listOf(pattern3),
                                            stopIds = setOf(stop2.id),
                                            upcomingTrips = listOf(
                                                objects.upcomingTrip(stop2Pattern1Prediction),
                                                objects.upcomingTrip(stop2Pattern3Prediction),
                                            ),
                                            allDataLoaded = false
                                        )
                                    )
                                )
                            )
                        )
            ),
            RouteCardData.ListBuilder().addStaticStopsData(listOf(stop1.id, stop2.id), global)
                .addUpcomingTrips(null, PredictionsStreamDataResponse(objects), filterAtTime = time, globalData = global)
                .data)
        }

    @Test
    fun `RouteCardData routeCardsForStopList hides rare direction with no predictions in next 120 min`() {
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
        // should not be included because not typical and prediction beyond 120 minutes
        val deviationInbound =
            objects.routePattern(route1) {
                directionId = 1
                sortOrder = 4
                typicality = RoutePattern.Typicality.Deviation
                representativeTrip { headsign = "Deviation In" }
            }

        val global = GlobalResponse(objects, patternIdsByStop = mapOf(stop1.id to listOf(typicalOutbound.id,
           deviationInbound.id)))

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
                departureTime = time + 121.minutes
                routeId = route1.id
                stopId = stop1.id
                tripId = deviationInbound.representativeTripId
            }
        assertEquals(
            listOf(RouteCardData(
                lineOrRoute = RouteCardData.LineOrRoute.Route(route1),
                stopData = listOf(
                    RouteCardData.RouteStopData(stop1, emptyList(), listOf(RouteCardData.Leaf(
                        directionId = 0,
                        routePatterns = listOf(typicalOutbound),
                        stopIds = setOf(stop1.id),
                        upcomingTrips = listOf(
                            objects.upcomingTrip(typicalOutboundPrediction),
                        ),
                        allDataLoaded = false,
                        alertsHere = emptyList()
                    ))))
            )),

            RouteCardData.routeCardsForStopList(listOf(stop1.id), global,
                filterAtTime = time,
                sortByDistanceFrom = null,
                schedules = null,
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(objects),
                pinnedRoutes = setOf(),
                context = RouteCardData.Context.NearbyTransit))
    }

    @Test
    fun `RouteCardData routeCardsForStopList shows rare direction with predictions in next 120 min`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()

        val route1 = objects.route()

        // should be included because typical and has prediction < 120 minutes
        val typicalOutbound =
            objects.routePattern(route1) {
                directionId = 0
                sortOrder = 1
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Typical Out" }
            }
        // should also be included even though > 120 minutes because above prediction < 120
        val deviationInbound =
            objects.routePattern(route1) {
                directionId = 1
                sortOrder = 4
                typicality = RoutePattern.Typicality.Deviation
                representativeTrip { headsign = "Deviation In" }
            }

        val deviationInboundTrip2 = objects.trip {
            directionId = 1
            routePatternId = deviationInbound.id
            routeId = route1.id
        }

        val global = GlobalResponse(objects, patternIdsByStop = mapOf(stop1.id to listOf(typicalOutbound.id,
            deviationInbound.id)))

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
                departureTime = time + 119.minutes
                routeId = route1.id
                stopId = stop1.id
                tripId = deviationInbound.representativeTripId
            }

        val deviationInboundPredictionLater =
            objects.prediction {
                departureTime = time + 121.minutes
                routeId = route1.id
                stopId = stop1.id
                tripId = deviationInboundTrip2.id
            }
        assertEquals(
            listOf(RouteCardData(
                lineOrRoute = RouteCardData.LineOrRoute.Route(route1),
                stopData = listOf(
                    RouteCardData.RouteStopData(stop1, emptyList(), listOf(RouteCardData.Leaf(
                        directionId = 0,
                        routePatterns = listOf(typicalOutbound),
                        stopIds = setOf(stop1.id),
                        upcomingTrips = listOf(
                            objects.upcomingTrip(typicalOutboundPrediction),
                        ),
                        allDataLoaded = false,
                        alertsHere = emptyList()
                    ),
                        RouteCardData.Leaf(
                            directionId = 1,
                            routePatterns = listOf(deviationInbound),
                            stopIds = setOf(stop1.id),
                            upcomingTrips = listOf(
                                objects.upcomingTrip(deviationInboundPrediction),
                                objects.upcomingTrip(deviationInboundPredictionLater),
                                ),
                            allDataLoaded = false,
                            alertsHere = emptyList()
                        )),
            )))),
            RouteCardData.routeCardsForStopList(listOf(stop1.id), global,
                filterAtTime = time,
                sortByDistanceFrom = null,
                schedules = null,
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(objects),
                pinnedRoutes = setOf(),
                context = RouteCardData.Context.NearbyTransit))
    }


    @Test
    fun `RouteCardData routeCardsForStopList handles schedule and predictions edge cases`()  {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()

        val route1 = objects.route()
        val route2 = objects.route()
        val route3 = objects.route()
        val route4 = objects.route()

        // assigning each route pattern to a unique route + direction to more easily test filtering
        // with one upcoming trip for each

        // exclude, schedule in past
        val schedulePast =
            objects.routePattern(route1) {
                sortOrder = 1
                representativeTrip { headsign = "Schedule Past" }
            }
        // include, schedule upcoming
        val scheduleSoon =
            objects.routePattern(route1) {
                directionId = 1
                sortOrder = 2
                representativeTrip { headsign = "Schedule Soon" }
            }
        // exclude, schedule too late
        val scheduleLater =
            objects.routePattern(route2) {
                sortOrder = 3
                representativeTrip { headsign = "Schedule Later" }
            }
        // exclude, prediction in past
        val predictionPast =
            objects.routePattern(route2) {
                directionId = 1
                sortOrder = 4
                representativeTrip { headsign = "Prediction Past" }
            }
        // include, prediction in past but BRD
        val predictionBrd =
            objects.routePattern(route3) {
                sortOrder = 5
                representativeTrip { headsign = "Prediction BRD" }
            }
        // include, prediction upcoming
        val predictionSoon =
            objects.routePattern(route3) {
                directionId = 1
                sortOrder = 6
                representativeTrip { headsign = "Prediction Soon" }
            }
        // exclude, prediction later
        val predictionLater =
            objects.routePattern(route4) {
                sortOrder = 7
                representativeTrip { headsign = "Prediction Later" }
            }
        val global = GlobalResponse(objects, patternIdsByStop = mapOf(stop1.id to listOf(schedulePast.id,
            scheduleSoon.id,
            scheduleLater.id,
            predictionPast.id,
            predictionBrd.id,
            predictionSoon.id,
            predictionLater.id)))

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
                departureTime = time + 121.minutes
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
                departureTime = time + 121.minutes
            }

        assertEquals(
            listOf(RouteCardData(
                lineOrRoute = RouteCardData.LineOrRoute.Route(route1),
                stopData = listOf(
                    RouteCardData.RouteStopData(stop1, emptyList(), listOf(RouteCardData.Leaf(
                        directionId = 1,
                        routePatterns = listOf(scheduleSoon),
                        stopIds = setOf(stop1.id),
                        upcomingTrips = listOf(
                            objects.upcomingTrip(scheduleSoonSchedule),
                        ),
                        allDataLoaded = true,
                        alertsHere = emptyList()

                    ))),
                    )),
                RouteCardData(
                    lineOrRoute = RouteCardData.LineOrRoute.Route(route3),
                    stopData = listOf(
                        RouteCardData.RouteStopData(stop1, emptyList(), listOf(RouteCardData.Leaf(
                            directionId = 0,
                            routePatterns = listOf(predictionBrd),
                            stopIds = setOf(stop1.id),
                            upcomingTrips = listOf(
                                objects.upcomingTrip(prediction = predictionBrdPrediction, vehicle = predictionBrdVehicle),
                            ),
                            allDataLoaded = true,
                            alertsHere = emptyList()

                        ),
                            RouteCardData.Leaf(
                                directionId = 1,
                                routePatterns = listOf(predictionSoon),
                                stopIds = setOf(stop1.id),
                                upcomingTrips = listOf(
                                    objects.upcomingTrip(predictionSoonPrediction),
                                ),
                                allDataLoaded = true,
                                alertsHere = emptyList()

                            ))
                    )),
                )),
             RouteCardData.routeCardsForStopList(
                 stopIds = listOf(stop1.id),
                globalData = global,
                sortByDistanceFrom = stop1.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
                filterAtTime = time,
                pinnedRoutes = setOf(),
                 context = RouteCardData.Context.NearbyTransit)
        )
    }

    @Test
    fun `RouteCardData routeCardsForStopList hides rare patterns while loading`()  {
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

        // should be excluded because no prediction
        val atypicalInbound =
            objects.routePattern(route1) {
                directionId = 1
                sortOrder = 6
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { headsign = "Atypical In" }
            }


    val global = GlobalResponse(objects, patternIdsByStop = mapOf(stop1.id to listOf(typicalOutbound.id,
        atypicalInbound.id)))

        val time = Instant.parse("2024-02-22T12:08:19-05:00")


        assertEquals(
            listOf(RouteCardData(
                lineOrRoute = RouteCardData.LineOrRoute.Route(route1),
                stopData = listOf(
                    RouteCardData.RouteStopData(stop1, emptyList(), listOf(RouteCardData.Leaf(
                        directionId = 0,
                        routePatterns = listOf(typicalOutbound),
                        stopIds = setOf(stop1.id),
                        upcomingTrips = listOf(),
                        allDataLoaded = false,
                        alertsHere = emptyList()

                    ))),
                ))),
            RouteCardData.routeCardsForStopList(
                stopIds = listOf(stop1.id),
                globalData = global,
                sortByDistanceFrom = stop1.position,
                schedules = null,
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
                filterAtTime = time,
                pinnedRoutes = setOf(),
                context = RouteCardData.Context.NearbyTransit)
        )
    }

    @Test
    fun `RouteCardData routCardsForStopList doesn't filter future trips in filtered stop details`() {
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

        // should be included because all trips should be visible in filtered stop details
        val deviationInbound =
            objects.routePattern(route1) {
                directionId = 1
                sortOrder = 4
                typicality = RoutePattern.Typicality.Deviation
                representativeTrip { headsign = "Deviation In" }
            }

        val global = GlobalResponse(objects, patternIdsByStop = mapOf(stop1.id to listOf(typicalOutbound.id, deviationInbound.id)))

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
                departureTime = time + 400.minutes
                routeId = route1.id
                stopId = stop1.id
                tripId = deviationInbound.representativeTripId
            }

        assertEquals(
            listOf(RouteCardData(
                lineOrRoute = RouteCardData.LineOrRoute.Route(route1),
                stopData = listOf(
                    RouteCardData.RouteStopData(stop1, emptyList(), listOf(RouteCardData.Leaf(
                        directionId = 0,
                        routePatterns = listOf(typicalOutbound),
                        stopIds = setOf(stop1.id),
                        upcomingTrips = listOf(objects.upcomingTrip(typicalOutboundPrediction)),
                        allDataLoaded = false,
                        alertsHere = emptyList()

                    ),
                        RouteCardData.Leaf(
                            directionId = 1,
                            routePatterns = listOf(deviationInbound),
                            stopIds = setOf(stop1.id),
                            upcomingTrips = listOf(objects.upcomingTrip(deviationInboundPrediction)),
                            allDataLoaded = false,
                            alertsHere = emptyList()

                        )),
                )))),
            RouteCardData.routeCardsForStopList(
                stopIds = listOf(stop1.id),
                globalData = global,
                sortByDistanceFrom = stop1.position,
                schedules = null,
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
                filterAtTime = time,
                pinnedRoutes = setOf(),
                context = RouteCardData.Context.StopDetailsFiltered
                )
        )
    }

    @Test
    fun `RouteCardData routeCardsForStopList includes cancellations in filtered stop details context`() {
        val objects = ObjectCollectionBuilder()
        val stop1 = objects.stop()
        val route1 = objects.route {
            type = RouteType.BUS
        }

        // should be included because typical and has cancelled prediction
        val typicalOutbound =
            objects.routePattern(route1) {
                directionId = 0
                sortOrder = 1
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Typical Out" }
            }

        val global = GlobalResponse(objects, patternIdsByStop = mapOf(stop1.id to listOf(typicalOutbound.id)))


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
            listOf(RouteCardData(
                lineOrRoute = RouteCardData.LineOrRoute.Route(route1),
                stopData = listOf(
                    RouteCardData.RouteStopData(stop1, emptyList(), listOf(RouteCardData.Leaf(
                        directionId = 0,
                        routePatterns = listOf(typicalOutbound),
                        stopIds = setOf(stop1.id),
                        upcomingTrips = listOf(objects.upcomingTrip(prediction = typicalOutboundPrediction, schedule = typicalOutboundSchedule)),
                        allDataLoaded = true,
                        alertsHere = emptyList()

                    )),
                    )))),
            RouteCardData.routeCardsForStopList(
                stopIds = listOf(stop1.id),
                globalData = global,
                sortByDistanceFrom = stop1.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
                filterAtTime = time,
                pinnedRoutes = setOf(),
                context = RouteCardData.Context.StopDetailsFiltered
            )
        )
    }

    @Test
    fun `RouteCardData routeCardsForStopList handles parent stops`()  {
        val objects = ObjectCollectionBuilder()
        val parentStop = objects.stop {
            childStopIds = listOf("childStop")
        }
        val childStop = objects.stop { id = "childStop"
            parentStationId = parentStop.id }
        val route1 = objects.route()
        val pattern1 = objects.routePattern(route1) { representativeTrip { headsign = "Harvard" } }

        val global = GlobalResponse(objects, patternIdsByStop = mapOf(childStop.id to listOf(pattern1.id)))
        val time = Instant.parse("2024-02-26T10:45:38-05:00")

        val prediction1 =
            objects.prediction {
                departureTime = time
                routeId = route1.id
                stopId = childStop.id
                tripId = pattern1.representativeTripId
            }

        assertEquals(
            listOf(RouteCardData(
                lineOrRoute = RouteCardData.LineOrRoute.Route(route1),
                stopData = listOf(
                    RouteCardData.RouteStopData(parentStop, emptyList(), listOf(RouteCardData.Leaf(
                        directionId = 0,
                        routePatterns = listOf(pattern1),
                        stopIds = setOf(parentStop.id, childStop.id),
                        upcomingTrips = listOf(objects.upcomingTrip(prediction1)),
                        allDataLoaded = false,
                        alertsHere = emptyList()

                    )),
                    )))),
            RouteCardData.routeCardsForStopList(
                stopIds = listOf(childStop.id, parentStop.id),
                globalData = global,
                sortByDistanceFrom = parentStop.position,
                schedules = null,
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(objects),
                filterAtTime = time,
                pinnedRoutes = setOf(),
                context = RouteCardData.Context.NearbyTransit)
        )
    }

    @Test
    fun `RouteCardData routeCardsForStopList incorporates schedules`()  {
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

        val global = GlobalResponse(objects, patternIdsByStop = mapOf(stop.id to listOf(routePattern.id)))


        assertEquals(
            listOf(RouteCardData(
                lineOrRoute = RouteCardData.LineOrRoute.Route(route),
                stopData = listOf(
                    RouteCardData.RouteStopData(stop, emptyList(), listOf(RouteCardData.Leaf(
                        directionId = 0,
                        routePatterns = listOf(routePattern),
                        stopIds = setOf(stop.id),
                        upcomingTrips = listOf(objects.upcomingTrip(prediction = pred1, schedule = sched1),
                            objects.upcomingTrip(prediction = pred2, schedule = sched2)),
                        allDataLoaded = true,
                        alertsHere = emptyList()

                    )),
                    )))),
            RouteCardData.routeCardsForStopList(
                stopIds = listOf(stop.id),
                globalData = global,
                sortByDistanceFrom = stop.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(objects),
                filterAtTime = time,
                pinnedRoutes = setOf(),
                context = RouteCardData.Context.NearbyTransit)
        )
    }

    @Test
    @Ignore // TODO: Add hasSchedules functionality as part of special service dates
    fun `RouteCardData routeCardsForStopList checks if any trips are scheduled all day`()  {
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
                pinnedRoutes = setOf(),)
        )
    }

    @Test
    fun `RouteCardData routeCardsForStopList checks route along with route pattern and stop`()  {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route1 = objects.route { sortOrder = 1 }
        val routePattern1 = objects.routePattern(route1) { representativeTrip { headsign = "A" } }
        val trip1 = objects.trip(routePattern1)

        val route2 = objects.route { sortOrder = 2 }
        val routePattern2 = objects.routePattern(route2) { representativeTrip { headsign = "B" } }
        val trip2 = objects.trip(routePattern2)

        // change from before: this will be included because it has a matching
        // route & direction, even if it is an unexpected route pattern.
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


        val global = GlobalResponse(objects, patternIdsByStop = mapOf(stop.id to listOf(routePattern1.id, routePattern2.id)))

        assertEquals(
            listOf(RouteCardData(
                lineOrRoute = RouteCardData.LineOrRoute.Route(route1),
                stopData = listOf(
                    RouteCardData.RouteStopData(stop, emptyList(), listOf(RouteCardData.Leaf(
                        directionId = 0,
                        routePatterns = listOf(routePattern1),
                        stopIds = setOf(stop.id),
                        upcomingTrips = listOf(objects.upcomingTrip(prediction = pred1, schedule = sched1)),
                        allDataLoaded = true,
                        alertsHere = emptyList()

                    )),
                    ))),
                RouteCardData(
                    lineOrRoute = RouteCardData.LineOrRoute.Route(route2),
                    stopData = listOf(
                        RouteCardData.RouteStopData(stop, emptyList(), listOf(RouteCardData.Leaf(
                            directionId = 0,
                            routePatterns = listOf(routePattern2),
                            stopIds = setOf(stop.id),
                            upcomingTrips = listOf(objects.upcomingTrip(prediction = pred2, schedule = sched2),
                                objects.upcomingTrip(prediction = pred3, schedule = sched3)),
                            allDataLoaded = true,
                            alertsHere = emptyList()

                        )),
                        )))),
            RouteCardData.routeCardsForStopList(
                stopIds = listOf(stop.id),
                globalData = global,
                sortByDistanceFrom = stop.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
                filterAtTime = time,
                pinnedRoutes = setOf(),
                context = RouteCardData.Context.NearbyTransit)
        )
    }

    @Test
    fun `RouteCardData routeCardsForStopList groups lines by direction`()  {
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

        // TODO: Use directions
        val directionWest = Direction("West", "Kenmore & West", 0)
        val directionEast = Direction("East", "Park St & North", 1)

        val global = GlobalResponse(objects,
            patternIdsByStop = mapOf(stop.id to listOf(routePatternB1.id,
                routePatternB2.id,
                routePatternC1.id,
                routePatternC2.id,
                routePatternE1.id,
                routePatternE2.id)))

        val expected =
            listOf(RouteCardData(
                lineOrRoute = RouteCardData.LineOrRoute.Line(line, setOf(routeB, routeC, routeE)),
                stopData = listOf(
                    RouteCardData.RouteStopData(stop, emptyList(), listOf(RouteCardData.Leaf(
                        directionId = 0,
                        routePatterns = listOf(routePatternB1, routePatternC1, routePatternE1),
                        stopIds = setOf(stop.id),
                        upcomingTrips = listOf(objects.upcomingTrip(prediction = predB1, schedule = schedB1),
                            objects.upcomingTrip(prediction = predC1, schedule = schedC1),
                            objects.upcomingTrip(prediction = predE1, schedule = schedE1)),
                        allDataLoaded = true,
                        alertsHere = emptyList()

                    ),RouteCardData.Leaf(
                        directionId = 1,
                        routePatterns = listOf(routePatternB2, routePatternC2, routePatternE2),
                        stopIds = setOf(stop.id),
                        upcomingTrips = listOf(objects.upcomingTrip(prediction = predB2, schedule = schedB2),
                            objects.upcomingTrip(prediction = predC2, schedule = schedC2),
                            objects.upcomingTrip(prediction = predE2, schedule = schedE2)),
                        allDataLoaded = true,
                        alertsHere = emptyList()

                    )
                        ),
                    ))))



        assertEquals(
            expected,
            RouteCardData.routeCardsForStopList(
                stopIds = listOf(stop.id),
                globalData = global,
                sortByDistanceFrom = stop.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
                filterAtTime = time,
                pinnedRoutes = setOf(),
                context = RouteCardData.Context.NearbyTransit)
        )
    }

    @Test
    fun `RouteCardData routeCardsForStopList north station disruption case`() {
        val objects = ObjectCollectionBuilder()
        val northStation =
            objects.stop {
                id = "place-north"
                locationType = LocationType.STATION
                childStopIds = listOf("70027", "70026")
            }

        val northStationNorthboundPlatform =
            objects.stop {
                id = "70027"
                locationType = LocationType.STOP
                parentStationId = "place-north"
            }

        val northStationSouthboundPlatform =
            objects.stop {
                id = "70026"
                locationType = LocationType.STOP
                parentStationId = "place-north"
            }

        val oakGrove =
            objects.stop {
                id = "place-ogmnl"
                locationType = LocationType.STATION
                childStopIds = listOf("70036")
            }

        val oakGrovePlatform =
            objects.stop {
                id = "70036"
                locationType = LocationType.STOP
                parentStationId = "place-ogmnl"
            }

        val forestHills =
            objects.stop {
                id = "place-forhl"
                locationType = LocationType.STATION
                childStopIds = listOf("70001")
            }

        val forestHillsPlatform =
            objects.stop {
                id = "70001"
                locationType = LocationType.STOP
                parentStationId = "place-forhl"
            }

        val orangeRoute = objects.route { id = "Orange" }
        val orangeNorthboundTypical =
            objects.routePattern(orangeRoute) {
                id = "Orange-3-1"
                typicality = RoutePattern.Typicality.Typical
                directionId = 1
                representativeTrip {
                    id = "canonical-Orange-C1-1"
                    headsign = "Oak Grove"
                    directionId = 1
                    stopIds = listOf("70001", "70027", "70036")
                }
            }


        val orangeNorthboundDiversion =
            objects.routePattern(orangeRoute) {
                id = "Orange-6-1"
                typicality = RoutePattern.Typicality.Diversion
                directionId = 1
                representativeTrip {
                    id = "65686489"
                    headsign = "North Station"
                    directionId = 1
                    stopIds = listOf("70001", "70027")
                }
            }

        val orangeSouthboundTypical =
            objects.routePattern(orangeRoute) {
                id = "Orange-3-0"
                typicality = RoutePattern.Typicality.Typical
                directionId = 0
                representativeTrip {
                    id = "canonical-Orange-C1-0"
                    headsign = "Forest Hills"
                    directionId = 0
                    stopIds = listOf("70036", "70026", "70001")
                }
            }

        val orangeSouthboundDiversion =
            objects.routePattern(orangeRoute) {
                id = "Orange-6-0"
                typicality = RoutePattern.Typicality.Diversion
                directionId = 0
                representativeTrip {
                    id = "65743311"
                    headsign = "Forest Hills"
                    directionId = 0
                    stopIds = listOf("70026", "70001")
                }
            }

        val orangeNorthboundTypicalTrip = objects.trip(orangeNorthboundTypical) {
            id = "northboundTypical"
        }

        val orangeNorthboundDiversionTrip = objects.trip(orangeNorthboundDiversion) {
            id = "northboundDiversion"
        }

        val orangeSouthboundDiversionTrip = objects.trip(orangeSouthboundDiversion)

        val time = Instant.parse("2024-10-30T16:40:00-04:00")

        // Scheduled northbound arrivals with North station as last stop
        val northboundSchedule =
            objects.schedule {
                trip = orangeNorthboundDiversionTrip
                stopId = northStationNorthboundPlatform.id
                stopSequence = 130
                arrivalTime = time + 4.minutes
                departureTime = null
            }

        val northboundPrediction =
            objects.prediction {
                trip = orangeNorthboundTypicalTrip
                stopId = northStationNorthboundPlatform.id
                stopSequence = 130
                arrivalTime = time + 8.minutes
                departureTime = null
            }

        // Scheduled southbound departures
        val southboundSchedule =
            objects.schedule {
                trip = orangeSouthboundDiversionTrip
                stopId = northStationSouthboundPlatform.id
                stopSequence = 60
                arrivalTime = null
                departureTime = time + 6.minutes
            }

        val alert =
            objects.alert {
                id = "601685"
                activePeriod(
                    Instant.parse("2024-10-28T03:00:00-04:00"),
                    Instant.parse("2024-11-02T03:00:00-04:00")
                )
                cause = Alert.Cause.Maintenance
                effect = Alert.Effect.Shuttle
                informedEntity =
                    mutableListOf(
                        // North station entities
                        Alert.InformedEntity(
                            activities =
                            listOf(
                                Alert.InformedEntity.Activity.Exit,
                                Alert.InformedEntity.Activity.Ride
                            ),
                            route = orangeRoute.id,
                            routeType = RouteType.HEAVY_RAIL,
                            stop = northStationSouthboundPlatform.id
                        ),
                        Alert.InformedEntity(
                            activities =
                            listOf(
                                Alert.InformedEntity.Activity.Board,
                                Alert.InformedEntity.Activity.Ride
                            ),
                            route = orangeRoute.id,
                            routeType = RouteType.HEAVY_RAIL,
                            stop = northStationNorthboundPlatform.id
                        ),
                        Alert.InformedEntity(
                            activities =
                            listOf(
                                Alert.InformedEntity.Activity.Board,
                                Alert.InformedEntity.Activity.Exit,
                                Alert.InformedEntity.Activity.Ride
                            ),
                            route = orangeRoute.id,
                            routeType = RouteType.HEAVY_RAIL,
                            stop = northStation.id
                        ),
                        Alert.InformedEntity(
                            activities =
                            listOf(
                                Alert.InformedEntity.Activity.Board,
                                Alert.InformedEntity.Activity.Exit,
                                Alert.InformedEntity.Activity.Ride
                            ),
                            route = orangeRoute.id,
                            routeType = RouteType.HEAVY_RAIL,
                            stop = oakGrove.id
                        ),
                        Alert.InformedEntity(
                            activities =
                            listOf(
                                Alert.InformedEntity.Activity.Board,
                                Alert.InformedEntity.Activity.Exit,
                                Alert.InformedEntity.Activity.Ride
                            ),
                            route = orangeRoute.id,
                            routeType = RouteType.HEAVY_RAIL,
                            stop = oakGrovePlatform.id
                        )
                    )
            }

        val global = GlobalResponse(objects )

        assertEquals(
            listOf(RouteCardData(
                lineOrRoute = RouteCardData.LineOrRoute.Route(orangeRoute),
                stopData = listOf(
                    RouteCardData.RouteStopData(northStation, emptyList(), listOf(RouteCardData.Leaf(
                        directionId = 0,
                        routePatterns = listOf(orangeSouthboundTypical, orangeSouthboundDiversion),
                        stopIds = setOf(northStation.id, northStationSouthboundPlatform.id, northStationNorthboundPlatform.id),
                        upcomingTrips = listOf(           UpcomingTrip(
                            trip = orangeSouthboundDiversionTrip,
                            schedule = southboundSchedule
                        )),
                        allDataLoaded = true,
                        // TODO: incorporate alert
                        alertsHere = emptyList()

                    ),RouteCardData.Leaf(
                        directionId = 1,
                        routePatterns = listOf(orangeNorthboundTypical, orangeNorthboundDiversion),
                        stopIds = setOf(northStation.id, northStationSouthboundPlatform.id, northStationNorthboundPlatform.id),
                        upcomingTrips = listOf(),
                        allDataLoaded = true,
                        alertsHere = emptyList()

                    )
                    ),
                    )))),
            RouteCardData.routeCardsForStopList(
                listOf(northStation.id, northStationSouthboundPlatform.id, northStationNorthboundPlatform.id),
                globalData = global,
                sortByDistanceFrom = northStation.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(mapOf(alert.id to alert)),
                filterAtTime = time,
                pinnedRoutes = setOf(),
                context = RouteCardData.Context.NearbyTransit
            )
        )
    }

    @Test
    fun `RouteCardData routeCardsForList filters out direction if it is the typical last stop of subway`() {
        val objects = ObjectCollectionBuilder()

        val oakGrove =
            objects.stop {
                id = "place-ogmnl"
                locationType = LocationType.STATION
                childStopIds = listOf("70036")
            }

        val orangeRoute = objects.route { id = "Orange" }
        val orangeNorthboundTypical =
            objects.routePattern(orangeRoute) {
                id = "Orange-3-1"
                typicality = RoutePattern.Typicality.Typical
                directionId = 1
                representativeTrip {
                    id = "canonical-Orange-C1-1"
                    headsign = "Oak Grove"
                    directionId = 1
                    stopIds = listOf("70001", "70027", "70036")
                }
            }

        val orangeSouthboundTypical =
            objects.routePattern(orangeRoute) {
                id = "Orange-3-0"
                typicality = RoutePattern.Typicality.Typical
                directionId = 0
                representativeTrip {
                    id = "canonical-Orange-C1-0"
                    headsign = "Forest Hills"
                    directionId = 0
                    stopIds = listOf("70036", "70026", "70001")
                }
            }

        val global = GlobalResponse(objects, patternIdsByStop = mapOf(oakGrove.id to listOf(orangeSouthboundTypical.id, orangeNorthboundTypical.id)))

        val time = Instant.parse("2024-10-30T16:40:00-04:00")

        val orangeNorthboundTypicalTrip = objects.trip(orangeNorthboundTypical)
        val orangeSouthboundTypicalTrip = objects.trip(orangeSouthboundTypical)

        val sched1 =
            objects.schedule {
                trip = orangeSouthboundTypicalTrip
                stopId = oakGrove.id
                stopSequence = 90
                departureTime = time + 1.minutes
            }
        val sched2 =
            objects.schedule {
                trip = orangeNorthboundTypicalTrip
                stopId = oakGrove.id
                stopSequence = 90
                arrivalTime = time + 2.minutes
                departureTime = null
            }

        assertEquals(
            listOf(RouteCardData(
                lineOrRoute = RouteCardData.LineOrRoute.Route(orangeRoute),
                stopData = listOf(
                    RouteCardData.RouteStopData(oakGrove, emptyList(), listOf(RouteCardData.Leaf(
                        directionId = 0,
                        routePatterns = listOf(orangeSouthboundTypical),
                        stopIds = setOf(oakGrove.id),
                        upcomingTrips = listOf(           UpcomingTrip(
                            trip = orangeSouthboundTypicalTrip,
                            schedule = sched1
                        )),
                        allDataLoaded = true,
                        alertsHere = emptyList()

                    )))))),
            RouteCardData.routeCardsForStopList(
                stopIds = listOf(oakGrove.id),
                globalData = global,
                sortByDistanceFrom = oakGrove.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
                filterAtTime = time,
                pinnedRoutes = setOf(),
                context = RouteCardData.Context.NearbyTransit
            )
        )
    }

    @Test
    fun `RouteCardData routeCardsForStopList filters out any arrival only for non-subway routes`() {
        val objects = ObjectCollectionBuilder()

        val longWharf =
            objects.stop {
                id = "Boat-Long"
            }

        val ferryRoute = objects.route {
            id = "Boat-F1"
            type = RouteType.FERRY
        }
        val ferryInboundToLongWharf =
            objects.routePattern(ferryRoute) {
                id = "Boat-F1-3-1"
                routeId = ferryRoute.id
                typicality = RoutePattern.Typicality.Typical
                directionId = 1
                representativeTrip {
                    id = "Boat-F1-0730-Hull-BF2H-01-Weekday-Fall-24"
                    headsign = "Long Wharf"
                    directionId = 1
                    stopIds = listOf("Boat-Hull", "Boat-Long")
                }
            }

        val ferryOutboundToHingham =
            objects.routePattern(ferryRoute) {
                id = "Boat-F1-0-0"
                routeId = ferryRoute.id
                typicality = RoutePattern.Typicality.Typical
                directionId = 0
                representativeTrip {
                    id = "Boat-F1-1100-Long-BF2H-01-Weekday-Fall-24"
                    headsign = "Hingham"
                    directionId = 0
                    stopIds = listOf("Boat-Long", "Boat-Logan", "Boat-Hull", "Boat-Hingham")
                }
            }

        val global = GlobalResponse(objects, patternIdsByStop = mapOf(longWharf.id to listOf(ferryInboundToLongWharf.id, ferryOutboundToHingham.id)))
        val time = Instant.parse("2024-10-30T16:40:00-04:00")

        val ferryInboundTrip = objects.trip(ferryInboundToLongWharf)
        val ferryOutboundTrip = objects.trip(ferryOutboundToHingham)

        val schedInbound =
            objects.schedule {
                trip = ferryInboundTrip
                stopId = longWharf.id
                stopSequence = 90
                arrivalTime = time + 2.minutes
                departureTime = null            }
        val schedOutbound =
            objects.schedule {
                trip = ferryOutboundTrip
                stopId = longWharf.id
                stopSequence = 90
                departureTime = time + 2.minutes

            }

        assertEquals(
            listOf(RouteCardData(
                lineOrRoute = RouteCardData.LineOrRoute.Route(ferryRoute),
                stopData = listOf(
                    RouteCardData.RouteStopData(longWharf, emptyList(), listOf(RouteCardData.Leaf(
                        directionId = 0,
                        routePatterns = listOf(ferryOutboundToHingham),
                        stopIds = setOf(longWharf.id),
                        upcomingTrips = listOf(objects.upcomingTrip(schedOutbound)),
                        allDataLoaded = true,
                        alertsHere = emptyList()

                    )))))),
            RouteCardData.routeCardsForStopList(
                stopIds = listOf(longWharf.id),
                globalData = global,
                sortByDistanceFrom = longWharf.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
                filterAtTime = time,
                pinnedRoutes = setOf(),
                context = RouteCardData.Context.NearbyTransit)
        )
    }

}
