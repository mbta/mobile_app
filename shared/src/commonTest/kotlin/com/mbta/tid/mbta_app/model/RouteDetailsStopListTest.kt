package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.RouteStopsResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class RouteDetailsStopListTest {
    @Test
    fun `RouteParameters finds available directions`() {
        val objects = ObjectCollectionBuilder()
        val route1 = objects.route()
        objects.routePattern(route1) { directionId = 0 }
        objects.routePattern(route1) { directionId = 0 }
        objects.routePattern(route1) { directionId = 0 }
        objects.routePattern(route1) { directionId = 0 }
        val route2 = objects.route()
        objects.routePattern(route2) { directionId = 0 }
        objects.routePattern(route2) { directionId = 1 }
        val line = objects.line()

        val globalData = GlobalResponse(objects)

        assertEquals(
            listOf(0),
            RouteDetailsStopList.RouteParameters(
                    RouteCardData.LineOrRoute.Route(route1),
                    globalData,
                )
                .availableDirections,
        )
        assertEquals(
            listOf(0, 1),
            RouteDetailsStopList.RouteParameters(
                    RouteCardData.LineOrRoute.Route(route2),
                    globalData,
                )
                .availableDirections,
        )
        assertEquals(
            listOf(0, 1),
            RouteDetailsStopList.RouteParameters(
                    RouteCardData.LineOrRoute.Line(line, setOf(route1, route2)),
                    globalData,
                )
                .availableDirections,
        )
    }

    @Test
    fun `RouteParameters finds direction info`() {
        val objects = ObjectCollectionBuilder()
        val route1 =
            objects.route {
                directionNames = listOf("East", "West")
                directionDestinations = listOf("Here", "There")
            }
        val route2 =
            objects.route {
                directionNames = listOf("East", "West")
                directionDestinations = listOf("Here", "Elsewhere")
            }
        val route3 =
            objects.route {
                directionNames = listOf("North", "South")
                directionDestinations = listOf("Somewhere", "Wherever")
            }
        val line = objects.line()

        val globalData = GlobalResponse(objects)

        assertEquals(
            listOf(Direction("East", "Here", 0), Direction("West", "There", 1)),
            RouteDetailsStopList.RouteParameters(
                    RouteCardData.LineOrRoute.Route(route1),
                    globalData,
                )
                .directions,
        )
        assertEquals(
            listOf(Direction("East", "Here", 0), Direction("West", "Elsewhere", 1)),
            RouteDetailsStopList.RouteParameters(
                    RouteCardData.LineOrRoute.Route(route2),
                    globalData,
                )
                .directions,
        )
        assertEquals(
            listOf(Direction("North", "Somewhere", 0), Direction("South", "Wherever", 1)),
            RouteDetailsStopList.RouteParameters(
                    RouteCardData.LineOrRoute.Route(route3),
                    globalData,
                )
                .directions,
        )
        assertEquals(
            listOf(Direction("East", "Here", 0), Direction("West", null, 1)),
            RouteDetailsStopList.RouteParameters(
                    RouteCardData.LineOrRoute.Line(line, setOf(route1, route2)),
                    globalData,
                )
                .directions,
        )
        assertEquals(
            listOf(Direction(null, null, 0), Direction(null, null, 1)),
            RouteDetailsStopList.RouteParameters(
                    RouteCardData.LineOrRoute.Line(line, setOf(route1, route2, route3)),
                    globalData,
                )
                .directions,
        )
    }

    @Test
    fun `getLineOrRoute gets route`() {
        val objects = ObjectCollectionBuilder()
        val ungroupedLine = objects.line()
        val route1 = objects.route()
        val route2 = objects.route()
        val route3 = objects.route { lineId = ungroupedLine.id }

        val globalData = GlobalResponse(objects)

        assertEquals(
            RouteCardData.LineOrRoute.Route(route1),
            RouteDetailsStopList.getLineOrRoute(route1.id, globalData),
        )
        assertEquals(
            RouteCardData.LineOrRoute.Route(route2),
            RouteDetailsStopList.getLineOrRoute(route2.id, globalData),
        )
        assertEquals(
            RouteCardData.LineOrRoute.Route(route3),
            RouteDetailsStopList.getLineOrRoute(route3.id, globalData),
        )
    }

    @Test
    fun `getLineOrRoute gets line excluding shuttles`() {
        val objects = ObjectCollectionBuilder()
        val line = objects.line { id = "line-Green" }
        val route1 = objects.route { lineId = line.id }
        val route2 = objects.route { lineId = line.id }
        val shuttleRoute =
            objects.route {
                id = "Shuttle-$id"
                lineId = line.id
            }
        assertTrue(shuttleRoute.isShuttle)

        val globalData = GlobalResponse(objects)

        assertEquals(
            RouteCardData.LineOrRoute.Line(line, setOf(route1, route2)),
            RouteDetailsStopList.getLineOrRoute(line.id, globalData),
        )
    }

    @Test
    fun `getLineOrRoute gets line if route in grouped line`() {
        val objects = ObjectCollectionBuilder()
        val line = objects.line { id = "line-Green" }
        val route1 = objects.route { lineId = line.id }
        val route2 = objects.route { lineId = line.id }

        val globalData = GlobalResponse(objects)

        assertEquals(
            RouteCardData.LineOrRoute.Line(line, setOf(route1, route2)),
            RouteDetailsStopList.getLineOrRoute(route1.id, globalData),
        )
        assertEquals(
            RouteCardData.LineOrRoute.Line(line, setOf(route1, route2)),
            RouteDetailsStopList.getLineOrRoute(route2.id, globalData),
        )
    }

    @Test
    fun `fromPieces finds transfer stops`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val connectingStop = objects.stop()
        val mainStop = objects.stop { connectingStopIds = listOf(connectingStop.id) }
        val mainRoute = objects.route()
        val connectingRoute = objects.route()

        val mainPattern =
            objects.routePattern(mainRoute) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { stopIds = listOf(mainStop.id) }
            }

        val connectingPattern =
            objects.routePattern(connectingRoute) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { stopIds = listOf(connectingStop.id) }
            }

        val globalData = GlobalResponse(objects)

        assertEquals(
            RouteDetailsStopList(
                listOf(
                    RouteDetailsStopList.Segment(
                        listOf(
                            RouteDetailsStopList.Entry(
                                mainStop,
                                connectingRoutes = listOf(connectingRoute),
                                patterns = listOf(mainPattern),
                            )
                        ),
                        hasRouteLine = true,
                    )
                )
            ),
            RouteDetailsStopList.fromPieces(
                mainRoute.id,
                RouteStopsResponse(listOf(mainStop.id)),
                globalData,
            ),
        )
    }

    @Test
    fun `fromPieces breaks segments by typicality`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val stop0 = objects.stop { id = "stop0" }
        val stop1 = objects.stop { id = "stop1" }
        val stop2NonTypical = objects.stop { id = "stop2NonTypical" }
        val stop3NonTypical = objects.stop { id = "stop3NonTypical" }
        val stop4 = objects.stop { id = "stop4" }
        val stop5NonTypical = objects.stop { id = "stop5NonTypical" }
        val stop6 = objects.stop { id = "stop6" }

        val mainRoute = objects.route()
        val patternTypical0 =
            objects.routePattern(mainRoute) {
                id = "typical_0"
                sortOrder = 0
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { stopIds = listOf(stop0.id, stop1.id, stop4.id) }
            }

        val patternTypical1 =
            objects.routePattern(mainRoute) {
                id = "typical_1"
                sortOrder = 1
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { stopIds = listOf(stop0.id, stop1.id, stop6.id) }
            }

        val patternNonTypical0 =
            objects.routePattern(mainRoute) {
                id = "non_typical_0"
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { stopIds = listOf(stop0.id, stop1.id, stop2NonTypical.id) }
            }

        val patternNonTypical1 =
            objects.routePattern(mainRoute) {
                id = "non_typical_1"
                typicality = RoutePattern.Typicality.Deviation
                representativeTrip {
                    stopIds =
                        listOf(stop0.id, stop1.id, stop3NonTypical.id, stop5NonTypical.id, stop6.id)
                }
            }

        val globalData = GlobalResponse(objects)

        assertEquals(
            RouteDetailsStopList(
                listOf(
                    RouteDetailsStopList.Segment(
                        listOf(
                            RouteDetailsStopList.Entry(
                                stop0,
                                connectingRoutes = listOf(),
                                patterns =
                                    listOf(
                                        patternTypical0,
                                        patternTypical1,
                                        patternNonTypical0,
                                        patternNonTypical1,
                                    ),
                            ),
                            RouteDetailsStopList.Entry(
                                stop1,
                                connectingRoutes = listOf(),
                                patterns =
                                    listOf(
                                        patternTypical0,
                                        patternTypical1,
                                        patternNonTypical0,
                                        patternNonTypical1,
                                    ),
                            ),
                        ),
                        hasRouteLine = true,
                    ),
                    RouteDetailsStopList.Segment(
                        listOf(
                            RouteDetailsStopList.Entry(
                                stop2NonTypical,
                                connectingRoutes = listOf(),
                                patterns = listOf(patternNonTypical0),
                            ),
                            RouteDetailsStopList.Entry(
                                stop3NonTypical,
                                connectingRoutes = listOf(),
                                patterns = listOf(patternNonTypical1),
                            ),
                        ),
                        hasRouteLine = false,
                    ),
                    RouteDetailsStopList.Segment(
                        listOf(
                            RouteDetailsStopList.Entry(
                                stop4,
                                connectingRoutes = listOf(),
                                patterns = listOf(patternTypical0),
                            )
                        ),
                        hasRouteLine = true,
                    ),
                    RouteDetailsStopList.Segment(
                        listOf(
                            RouteDetailsStopList.Entry(
                                stop5NonTypical,
                                connectingRoutes = listOf(),
                                patterns = listOf(patternNonTypical1),
                            )
                        ),
                        hasRouteLine = false,
                    ),
                    RouteDetailsStopList.Segment(
                        listOf(
                            RouteDetailsStopList.Entry(
                                stop6,
                                connectingRoutes = listOf(),
                                patterns = listOf(patternTypical1, patternNonTypical1),
                            )
                        ),
                        hasRouteLine = false,
                    ),
                )
            ),
            RouteDetailsStopList.fromPieces(
                mainRoute.id,
                RouteStopsResponse(
                    listOf(
                        stop0.id,
                        stop1.id,
                        stop2NonTypical.id,
                        stop3NonTypical.id,
                        stop4.id,
                        stop5NonTypical.id,
                        stop6.id,
                    )
                ),
                globalData,
            ),
        )
    }
}
