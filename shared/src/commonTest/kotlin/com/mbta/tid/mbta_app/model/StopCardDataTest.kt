package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.test.Test
import kotlin.test.assertEquals

class StopCardDataTest {
    @Test
    fun `fromRouteCardData groups by stop`() {
        val now = EasternTimeInstant.now()
        val objects = TestData.clone()
        val ruggles = objects.getStop("place-rugg")
        val tremontAtMelneaCass =
            objects.stop {
                id = "1227"
                name = "Tremont St @ Melnea Cass Blvd"
            }
        val boylston = objects.getStop("place-boyls")
        val ol = LineOrRoute.Route(objects.getRoute("Orange"))
        val gl =
            LineOrRoute.Line(
                objects.getLine("line-Green"),
                setOf(
                    objects.getRoute("Green-B"),
                    objects.getRoute("Green-C"),
                    objects.getRoute("Green-D"),
                    objects.getRoute("Green-E"),
                ),
            )
        val bus43 =
            LineOrRoute.Route(
                objects.route {
                    id = "43"
                    directionNames = listOf("Outbound", "Inbound")
                    directionDestinations = listOf("Ruggles Station", "Park Street Station")
                }
            )
        val bus43Inbound =
            objects.routePattern(bus43.route) {
                directionId = 1
                sortOrder = 504301000
                typicality = RoutePattern.Typicality.Typical
                representativeTrip {
                    headsign = "Park St & Tremont St"
                    stopIds = listOf("17869", "1227", "10000")
                }
            }

        val global = GlobalResponse(objects)

        val olSouthboundLeaf =
            RouteCardData.Leaf(
                ol,
                ruggles,
                0,
                listOf(objects.getRoutePattern("Orange-3-0")),
                ruggles.childStopIds.toSet(),
                upcomingTrips = emptyList(),
                alertsHere = emptyList(),
                allDataLoaded = true,
                hasSchedulesToday = true,
                subwayServiceStartTime = null,
                alertsDownstream = emptyList(),
                RouteCardData.Context.Favorites,
            )

        val olNorthboundLeaf =
            RouteCardData.Leaf(
                ol,
                ruggles,
                1,
                listOf(objects.getRoutePattern("Orange-3-1")),
                ruggles.childStopIds.toSet(),
                upcomingTrips = emptyList(),
                alertsHere = emptyList(),
                allDataLoaded = true,
                hasSchedulesToday = true,
                subwayServiceStartTime = null,
                alertsDownstream = emptyList(),
                RouteCardData.Context.Favorites,
            )

        val bus43RugglesLeaf =
            RouteCardData.Leaf(
                bus43,
                ruggles,
                1,
                listOf(bus43Inbound),
                ruggles.childStopIds.toSet(),
                upcomingTrips = emptyList(),
                alertsHere = emptyList(),
                allDataLoaded = true,
                hasSchedulesToday = true,
                subwayServiceStartTime = null,
                alertsDownstream = emptyList(),
                RouteCardData.Context.Favorites,
            )

        val bus43TremontAtMelneaCassLeaf =
            RouteCardData.Leaf(
                bus43,
                tremontAtMelneaCass,
                1,
                listOf(bus43Inbound),
                setOf(tremontAtMelneaCass.id),
                upcomingTrips = emptyList(),
                alertsHere = emptyList(),
                allDataLoaded = true,
                hasSchedulesToday = true,
                subwayServiceStartTime = null,
                alertsDownstream = emptyList(),
                RouteCardData.Context.Favorites,
            )

        val glWestboundLeaf =
            RouteCardData.Leaf(
                gl,
                boylston,
                0,
                listOf(
                    objects.getRoutePattern("Green-B-812-0"),
                    objects.getRoutePattern("Green-C-832-0"),
                    objects.getRoutePattern("Green-D-855-0"),
                    objects.getRoutePattern("Green-E-886-0"),
                ),
                boylston.childStopIds.toSet(),
                upcomingTrips = emptyList(),
                alertsHere = emptyList(),
                allDataLoaded = true,
                hasSchedulesToday = true,
                subwayServiceStartTime = null,
                alertsDownstream = emptyList(),
                RouteCardData.Context.Favorites,
            )

        val routeCardData =
            listOf(
                RouteCardData(
                    ol,
                    listOf(
                        RouteCardData.RouteStopData(
                            ol,
                            ruggles,
                            listOf(olSouthboundLeaf, olNorthboundLeaf),
                            global,
                        )
                    ),
                    now,
                ),
                RouteCardData(
                    bus43,
                    listOf(
                        RouteCardData.RouteStopData(
                            bus43,
                            ruggles,
                            listOf(bus43RugglesLeaf),
                            global,
                        ),
                        RouteCardData.RouteStopData(
                            bus43,
                            tremontAtMelneaCass,
                            listOf(bus43TremontAtMelneaCassLeaf),
                            global,
                        ),
                    ),
                    now,
                ),
                RouteCardData(
                    gl,
                    listOf(
                        RouteCardData.RouteStopData(gl, boylston, listOf(glWestboundLeaf), global)
                    ),
                    now,
                ),
            )

        assertEquals(
            listOf(
                StopCardData(ruggles, listOf(olSouthboundLeaf, olNorthboundLeaf, bus43RugglesLeaf)),
                StopCardData(tremontAtMelneaCass, listOf(bus43TremontAtMelneaCassLeaf)),
                StopCardData(boylston, listOf(glWestboundLeaf)),
            ),
            StopCardData.fromRouteCardData(routeCardData, sortByDistanceFrom = null),
        )
    }
}
