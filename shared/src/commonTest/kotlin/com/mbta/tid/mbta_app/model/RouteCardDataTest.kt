package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

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
                                            routePatterns = listOf(route1rp1),
                                            stopIds = setOf(stop1.id)
                                        )
                                    )
                                ),
                                stop2.id to RouteCardData.RouteStopDataBuilder(
                                    stop2, emptyList(), mapOf(
                                        0 to RouteCardData.LeafBuilder(
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
                                            routePatterns = listOf(
                                                route1rp1,
                                                route1rp2
                                            )
                                            ,
                                            stopIds = setOf(stop1.id)
                                        ),
                                        1 to RouteCardData.LeafBuilder(
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
                                            routePatterns = listOf(routeDrp1, routeBrp1, routeCrp1 ),
                                            stopIds = setOf(stopGov.id)
                                        ),
                                        1 to RouteCardData.LeafBuilder(
                                            routePatterns = listOf(routeDrp2, routeCrp3 ),
                                            stopIds = setOf(stopGov.id)
                                        )
                                    )
                                ),
                            )
                        )
            ),
            RouteCardData.ListBuilder().addStaticStopsData(nearby.stopIds, global).data
        )


    }

}
