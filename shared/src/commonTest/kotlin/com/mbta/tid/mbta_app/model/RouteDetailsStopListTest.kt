package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.RouteStopsResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
    fun `twistedConnections handles branch starting`() {
        val objects = ObjectCollectionBuilder()
        val stop1 = objects.stop()
        val stop2 = objects.stop()
        val stop3 = objects.stop()
        val lane = RouteBranchSegment.Lane.Left
        assertEquals(
            null,
            RouteDetailsStopList.Segment(
                    listOf(
                        RouteDetailsStopList.Entry(
                            stop1,
                            lane,
                            stickConnections =
                                RouteBranchSegment.StickConnection.forward(
                                    null,
                                    stop1.id,
                                    stop2.id,
                                    lane,
                                ),
                            connectingRoutes = emptyList(),
                        ),
                        RouteDetailsStopList.Entry(
                            stop2,
                            lane,
                            stickConnections =
                                RouteBranchSegment.StickConnection.forward(
                                    stop1.id,
                                    stop2.id,
                                    stop3.id,
                                    lane,
                                ),
                            connectingRoutes = emptyList(),
                        ),
                    ),
                    isTypical = false,
                )
                .twistedConnections(),
        )
    }

    @Test
    fun `twistedConnections handles branch ending`() {
        val objects = ObjectCollectionBuilder()
        val stop1 = objects.stop()
        val stop2 = objects.stop()
        val stop3 = objects.stop()
        val lane = RouteBranchSegment.Lane.Left
        assertEquals(
            null,
            RouteDetailsStopList.Segment(
                    listOf(
                        RouteDetailsStopList.Entry(
                            stop2,
                            lane,
                            stickConnections =
                                RouteBranchSegment.StickConnection.forward(
                                    stop1.id,
                                    stop2.id,
                                    stop3.id,
                                    lane,
                                ),
                            connectingRoutes = emptyList(),
                        ),
                        RouteDetailsStopList.Entry(
                            stop3,
                            lane,
                            stickConnections =
                                RouteBranchSegment.StickConnection.forward(
                                    stop2.id,
                                    stop3.id,
                                    null,
                                    lane,
                                ),
                            connectingRoutes = emptyList(),
                        ),
                    ),
                    isTypical = false,
                )
                .twistedConnections(),
        )
    }

    @Test
    fun `twistedConnections handles branch continuing and skipping`() {
        val objects = ObjectCollectionBuilder()
        val stop1 = objects.stop()
        val stop2 = objects.stop()
        val stop3 = objects.stop()
        val stop4 = objects.stop()
        val stopLane = RouteBranchSegment.Lane.Left
        val skipLane = RouteBranchSegment.Lane.Right
        val skip = RouteBranchSegment.StickConnection.forward(stop1.id, null, stop4.id, skipLane)
        assertEquals(
            listOf(
                Pair(
                    RouteBranchSegment.StickConnection(
                        fromStop = stop1.id,
                        toStop = stop4.id,
                        fromLane = skipLane,
                        toLane = skipLane,
                        fromVPos = RouteBranchSegment.VPos.Top,
                        toVPos = RouteBranchSegment.VPos.Bottom,
                    ),
                    false,
                ),
                Pair(
                    RouteBranchSegment.StickConnection(
                        fromStop = stop1.id,
                        toStop = stop4.id,
                        fromLane = stopLane,
                        toLane = stopLane,
                        fromVPos = RouteBranchSegment.VPos.Top,
                        toVPos = RouteBranchSegment.VPos.Bottom,
                    ),
                    true,
                ),
            ),
            RouteDetailsStopList.Segment(
                    listOf(
                        RouteDetailsStopList.Entry(
                            stop2,
                            stopLane,
                            stickConnections =
                                RouteBranchSegment.StickConnection.forward(
                                    stop1.id,
                                    stop2.id,
                                    stop3.id,
                                    stopLane,
                                ) + skip,
                            connectingRoutes = emptyList(),
                        ),
                        RouteDetailsStopList.Entry(
                            stop3,
                            stopLane,
                            stickConnections =
                                RouteBranchSegment.StickConnection.forward(
                                    stop2.id,
                                    stop3.id,
                                    stop4.id,
                                    stopLane,
                                ) + skip,
                            connectingRoutes = emptyList(),
                        ),
                    ),
                    isTypical = false,
                )
                .twistedConnections(),
        )
    }

    @Test
    fun `twistedConnections handles the 33 inbound case`() {
        val objects = ObjectCollectionBuilder()
        val parentRight = objects.stop()
        val parentCenter = objects.stop()
        val parentLeft = objects.stop()
        val stop1 = objects.stop()
        val stop2 = objects.stop()
        val stop3 = objects.stop()
        val child = objects.stop()
        val skip =
            RouteBranchSegment.StickConnection.forward(
                parentLeft.id,
                null,
                child.id,
                RouteBranchSegment.Lane.Left,
            )
        val segment =
            RouteDetailsStopList.Segment(
                listOf(
                    RouteDetailsStopList.Entry(
                        stop1,
                        RouteBranchSegment.Lane.Right,
                        RouteBranchSegment.StickConnection.forward(
                            parentRight.id,
                            stop1.id,
                            stop2.id,
                            RouteBranchSegment.Lane.Right,
                        ) +
                            skip +
                            RouteBranchSegment.StickConnection(
                                fromStop = parentCenter.id,
                                toStop = stop1.id,
                                fromLane = RouteBranchSegment.Lane.Center,
                                toLane = RouteBranchSegment.Lane.Right,
                                fromVPos = RouteBranchSegment.VPos.Top,
                                toVPos = RouteBranchSegment.VPos.Center,
                            ),
                        connectingRoutes = emptyList(),
                    ),
                    RouteDetailsStopList.Entry(
                        stop2,
                        RouteBranchSegment.Lane.Right,
                        RouteBranchSegment.StickConnection.forward(
                            stop1.id,
                            stop2.id,
                            stop3.id,
                            RouteBranchSegment.Lane.Right,
                        ),
                        connectingRoutes = emptyList(),
                    ),
                    RouteDetailsStopList.Entry(
                        stop3,
                        RouteBranchSegment.Lane.Right,
                        RouteBranchSegment.StickConnection.forward(
                            stop2.id,
                            stop3.id,
                            child.id,
                            RouteBranchSegment.Lane.Right,
                        ),
                        connectingRoutes = emptyList(),
                    ),
                ),
                isTypical = false,
            )
        assertEquals(
            listOf(
                Pair(skip.single(), false),
                Pair(
                    RouteBranchSegment.StickConnection(
                        fromStop = parentRight.id,
                        toStop = child.id,
                        fromLane = RouteBranchSegment.Lane.Right,
                        toLane = RouteBranchSegment.Lane.Right,
                        fromVPos = RouteBranchSegment.VPos.Top,
                        toVPos = RouteBranchSegment.VPos.Bottom,
                    ),
                    true,
                ),
                Pair(
                    RouteBranchSegment.StickConnection(
                        fromStop = parentCenter.id,
                        toStop = child.id,
                        fromLane = RouteBranchSegment.Lane.Center,
                        toLane = RouteBranchSegment.Lane.Right,
                        fromVPos = RouteBranchSegment.VPos.Top,
                        toVPos = RouteBranchSegment.VPos.Bottom,
                    ),
                    false,
                ),
            ),
            segment.twistedConnections(),
        )
    }

    @Test
    fun `fromPieces finds transfer stops`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val connectingStop = objects.stop()
        val mainStop = objects.stop { connectingStopIds = listOf(connectingStop.id) }
        val mainRoute = objects.route()
        val connectingRoute = objects.route()

        objects.routePattern(mainRoute) {
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { stopIds = listOf(mainStop.id) }
        }

        objects.routePattern(connectingRoute) {
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { stopIds = listOf(connectingStop.id) }
        }

        val globalData = GlobalResponse(objects)

        assertEquals(
            RouteDetailsStopList(
                0,
                listOf(
                    RouteDetailsStopList.Segment(
                        listOf(
                            RouteDetailsStopList.Entry(
                                mainStop,
                                RouteBranchSegment.Lane.Center,
                                stickConnections = emptyList(),
                                connectingRoutes = listOf(connectingRoute),
                            )
                        ),
                        isTypical = true,
                    )
                ),
            ),
            RouteDetailsStopList.fromPieces(
                mainRoute.id,
                0,
                RouteStopsResult(
                    mainRoute.id,
                    0,
                    listOf(
                        RouteBranchSegment(
                            listOf(
                                RouteBranchSegment.BranchStop(
                                    mainStop.id,
                                    RouteBranchSegment.Lane.Center,
                                    connections = emptyList(),
                                )
                            ),
                            name = null,
                            isTypical = true,
                        )
                    ),
                ),
                globalData,
            ),
        )
    }

    @Test
    fun `fromPieces returns null if direction doesn't match`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val mainStop = objects.stop {}
        val mainRoute = objects.route()

        val globalData = GlobalResponse(objects)

        assertNull(
            RouteDetailsStopList.fromPieces(
                mainRoute.id,
                0,
                RouteStopsResult(
                    mainRoute.id,
                    1,
                    listOf(
                        RouteBranchSegment(
                            listOf(
                                RouteBranchSegment.BranchStop(
                                    mainStop.id,
                                    RouteBranchSegment.Lane.Center,
                                    emptyList(),
                                )
                            ),
                            name = null,
                            isTypical = true,
                        )
                    ),
                ),
                globalData,
            )
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

        val globalData = GlobalResponse(objects)

        assertEquals(
            RouteDetailsStopList(
                0,
                listOf(
                    RouteDetailsStopList.Segment(
                        listOf(
                            RouteDetailsStopList.Entry(
                                stop0,
                                RouteBranchSegment.Lane.Center,
                                stickConnections = emptyList(),
                                connectingRoutes = listOf(),
                            ),
                            RouteDetailsStopList.Entry(
                                stop1,
                                RouteBranchSegment.Lane.Center,
                                stickConnections = emptyList(),
                                connectingRoutes = listOf(),
                            ),
                        ),
                        isTypical = true,
                    ),
                    RouteDetailsStopList.Segment(
                        listOf(
                            RouteDetailsStopList.Entry(
                                stop2NonTypical,
                                RouteBranchSegment.Lane.Center,
                                stickConnections = emptyList(),
                                connectingRoutes = listOf(),
                            ),
                            RouteDetailsStopList.Entry(
                                stop3NonTypical,
                                RouteBranchSegment.Lane.Center,
                                stickConnections = emptyList(),
                                connectingRoutes = listOf(),
                            ),
                        ),
                        isTypical = false,
                    ),
                    RouteDetailsStopList.Segment(
                        listOf(
                            RouteDetailsStopList.Entry(
                                stop4,
                                RouteBranchSegment.Lane.Center,
                                stickConnections = emptyList(),
                                connectingRoutes = listOf(),
                            )
                        ),
                        isTypical = true,
                    ),
                    RouteDetailsStopList.Segment(
                        listOf(
                            RouteDetailsStopList.Entry(
                                stop5NonTypical,
                                RouteBranchSegment.Lane.Center,
                                stickConnections = emptyList(),
                                connectingRoutes = listOf(),
                            )
                        ),
                        isTypical = false,
                    ),
                    RouteDetailsStopList.Segment(
                        listOf(
                            RouteDetailsStopList.Entry(
                                stop6,
                                RouteBranchSegment.Lane.Center,
                                stickConnections = emptyList(),
                                connectingRoutes = listOf(),
                            )
                        ),
                        isTypical = true,
                    ),
                ),
            ),
            RouteDetailsStopList.fromPieces(
                mainRoute.id,
                0,
                RouteStopsResult(
                    mainRoute.id,
                    0,
                    listOf(
                        RouteBranchSegment(
                            listOf(
                                RouteBranchSegment.BranchStop(
                                    stop0.id,
                                    RouteBranchSegment.Lane.Center,
                                    emptyList(),
                                ),
                                RouteBranchSegment.BranchStop(
                                    stop1.id,
                                    RouteBranchSegment.Lane.Center,
                                    emptyList(),
                                ),
                            ),
                            name = null,
                            isTypical = true,
                        ),
                        RouteBranchSegment(
                            listOf(
                                RouteBranchSegment.BranchStop(
                                    stop2NonTypical.id,
                                    RouteBranchSegment.Lane.Center,
                                    emptyList(),
                                )
                            ),
                            name = null,
                            isTypical = false,
                        ),
                        RouteBranchSegment(
                            listOf(
                                RouteBranchSegment.BranchStop(
                                    stop3NonTypical.id,
                                    RouteBranchSegment.Lane.Center,
                                    emptyList(),
                                )
                            ),
                            name = null,
                            isTypical = false,
                        ),
                        RouteBranchSegment(
                            listOf(
                                RouteBranchSegment.BranchStop(
                                    stop4.id,
                                    RouteBranchSegment.Lane.Center,
                                    emptyList(),
                                )
                            ),
                            name = null,
                            isTypical = true,
                        ),
                        RouteBranchSegment(
                            listOf(
                                RouteBranchSegment.BranchStop(
                                    stop5NonTypical.id,
                                    RouteBranchSegment.Lane.Center,
                                    emptyList(),
                                )
                            ),
                            name = null,
                            isTypical = false,
                        ),
                        RouteBranchSegment(
                            listOf(
                                RouteBranchSegment.BranchStop(
                                    stop6.id,
                                    RouteBranchSegment.Lane.Center,
                                    emptyList(),
                                )
                            ),
                            name = null,
                            isTypical = true,
                        ),
                    ),
                ),
                globalData,
            ),
        )
    }
}
