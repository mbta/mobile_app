package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.NewRouteStopsResult
import com.mbta.tid.mbta_app.repositories.OldRouteStopsResult
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
            listOf(
                Pair(
                    RouteBranchSegment.StickConnection(
                        fromStop = stop2.id,
                        toStop = stop3.id,
                        fromLane = lane,
                        toLane = lane,
                        fromVPos = RouteBranchSegment.VPos.Center,
                        toVPos = RouteBranchSegment.VPos.Bottom,
                    ),
                    true,
                )
            ),
            RouteDetailsStopList.NewSegment(
                    listOf(
                        RouteDetailsStopList.NewEntry(
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
                        RouteDetailsStopList.NewEntry(
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
            listOf(
                Pair(
                    RouteBranchSegment.StickConnection(
                        fromStop = stop1.id,
                        toStop = stop2.id,
                        fromLane = lane,
                        toLane = lane,
                        fromVPos = RouteBranchSegment.VPos.Top,
                        toVPos = RouteBranchSegment.VPos.Center,
                    ),
                    true,
                )
            ),
            RouteDetailsStopList.NewSegment(
                    listOf(
                        RouteDetailsStopList.NewEntry(
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
                        RouteDetailsStopList.NewEntry(
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
            RouteDetailsStopList.NewSegment(
                    listOf(
                        RouteDetailsStopList.NewEntry(
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
                        RouteDetailsStopList.NewEntry(
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
            RouteDetailsStopList.NewSegment(
                listOf(
                    RouteDetailsStopList.NewEntry(
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
                    RouteDetailsStopList.NewEntry(
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
                    RouteDetailsStopList.NewEntry(
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
    fun `fromOldPieces finds transfer stops`() = runBlocking {
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
                0,
                listOf(
                    RouteDetailsStopList.OldSegment(
                        listOf(
                            RouteDetailsStopList.OldEntry(
                                mainStop,
                                connectingRoutes = listOf(connectingRoute),
                                patterns = listOf(mainPattern),
                            )
                        ),
                        hasRouteLine = true,
                    )
                ),
                null,
            ),
            RouteDetailsStopList.fromOldPieces(
                mainRoute.id,
                0,
                OldRouteStopsResult(mainRoute.id, 0, listOf(mainStop.id)),
                globalData,
            ),
        )
    }

    @Test
    fun `fromNewPieces finds transfer stops`() = runBlocking {
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
                null,
                listOf(
                    RouteDetailsStopList.NewSegment(
                        listOf(
                            RouteDetailsStopList.NewEntry(
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
            RouteDetailsStopList.fromNewPieces(
                mainRoute.id,
                0,
                NewRouteStopsResult(
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
    fun `fromOldPieces returns null if direction doesn't match`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val mainStop = objects.stop {}
        val mainRoute = objects.route()

        val globalData = GlobalResponse(objects)

        assertNull(
            RouteDetailsStopList.fromOldPieces(
                mainRoute.id,
                0,
                OldRouteStopsResult(mainRoute.id, 1, listOf(mainStop.id)),
                globalData,
            )
        )
    }

    @Test
    fun `fromNewPieces returns null if direction doesn't match`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val mainStop = objects.stop {}
        val mainRoute = objects.route()

        val globalData = GlobalResponse(objects)

        assertNull(
            RouteDetailsStopList.fromNewPieces(
                mainRoute.id,
                0,
                NewRouteStopsResult(
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
    fun `fromOldPieces breaks segments by typicality`() = runBlocking {
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
                0,
                listOf(
                    RouteDetailsStopList.OldSegment(
                        listOf(
                            RouteDetailsStopList.OldEntry(
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
                            RouteDetailsStopList.OldEntry(
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
                    RouteDetailsStopList.OldSegment(
                        listOf(
                            RouteDetailsStopList.OldEntry(
                                stop2NonTypical,
                                connectingRoutes = listOf(),
                                patterns = listOf(patternNonTypical0),
                            ),
                            RouteDetailsStopList.OldEntry(
                                stop3NonTypical,
                                connectingRoutes = listOf(),
                                patterns = listOf(patternNonTypical1),
                            ),
                        ),
                        hasRouteLine = false,
                    ),
                    RouteDetailsStopList.OldSegment(
                        listOf(
                            RouteDetailsStopList.OldEntry(
                                stop4,
                                connectingRoutes = listOf(),
                                patterns = listOf(patternTypical0),
                            )
                        ),
                        hasRouteLine = true,
                    ),
                    RouteDetailsStopList.OldSegment(
                        listOf(
                            RouteDetailsStopList.OldEntry(
                                stop5NonTypical,
                                connectingRoutes = listOf(),
                                patterns = listOf(patternNonTypical1),
                            )
                        ),
                        hasRouteLine = false,
                    ),
                    RouteDetailsStopList.OldSegment(
                        listOf(
                            RouteDetailsStopList.OldEntry(
                                stop6,
                                connectingRoutes = listOf(),
                                patterns = listOf(patternTypical1, patternNonTypical1),
                            )
                        ),
                        hasRouteLine = false,
                    ),
                ),
                null,
            ),
            RouteDetailsStopList.fromOldPieces(
                mainRoute.id,
                0,
                OldRouteStopsResult(
                    mainRoute.id,
                    0,
                    listOf(
                        stop0.id,
                        stop1.id,
                        stop2NonTypical.id,
                        stop3NonTypical.id,
                        stop4.id,
                        stop5NonTypical.id,
                        stop6.id,
                    ),
                ),
                globalData,
            ),
        )
    }

    @Test
    fun `fromNewPieces breaks segments by typicality`() = runBlocking {
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
                null,
                listOf(
                    RouteDetailsStopList.NewSegment(
                        listOf(
                            RouteDetailsStopList.NewEntry(
                                stop0,
                                RouteBranchSegment.Lane.Center,
                                stickConnections = emptyList(),
                                connectingRoutes = listOf(),
                            ),
                            RouteDetailsStopList.NewEntry(
                                stop1,
                                RouteBranchSegment.Lane.Center,
                                stickConnections = emptyList(),
                                connectingRoutes = listOf(),
                            ),
                        ),
                        isTypical = true,
                    ),
                    RouteDetailsStopList.NewSegment(
                        listOf(
                            RouteDetailsStopList.NewEntry(
                                stop2NonTypical,
                                RouteBranchSegment.Lane.Center,
                                stickConnections = emptyList(),
                                connectingRoutes = listOf(),
                            ),
                            RouteDetailsStopList.NewEntry(
                                stop3NonTypical,
                                RouteBranchSegment.Lane.Center,
                                stickConnections = emptyList(),
                                connectingRoutes = listOf(),
                            ),
                        ),
                        isTypical = false,
                    ),
                    RouteDetailsStopList.NewSegment(
                        listOf(
                            RouteDetailsStopList.NewEntry(
                                stop4,
                                RouteBranchSegment.Lane.Center,
                                stickConnections = emptyList(),
                                connectingRoutes = listOf(),
                            )
                        ),
                        isTypical = true,
                    ),
                    RouteDetailsStopList.NewSegment(
                        listOf(
                            RouteDetailsStopList.NewEntry(
                                stop5NonTypical,
                                RouteBranchSegment.Lane.Center,
                                stickConnections = emptyList(),
                                connectingRoutes = listOf(),
                            )
                        ),
                        isTypical = false,
                    ),
                    RouteDetailsStopList.NewSegment(
                        listOf(
                            RouteDetailsStopList.NewEntry(
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
            RouteDetailsStopList.fromNewPieces(
                mainRoute.id,
                0,
                NewRouteStopsResult(
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

    @Test
    fun `fromOldPieces breaks segments by typicality in the selected direction`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val stop0 = objects.stop { id = "stop0" }
        val stop1 = objects.stop { id = "stop1" }
        val stop2 = objects.stop { id = "stop2" }

        val mainRoute = objects.route()

        val patternTypical0 =
            objects.routePattern(mainRoute) {
                id = "typical_0"
                directionId = 0
                sortOrder = 1
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { stopIds = listOf(stop0.id, stop1.id, stop2.id) }
            }

        val patternTypicalOppositeDirection =
            objects.routePattern(mainRoute) {
                id = "typical_opposite_direction"
                directionId = 1
                sortOrder = 0
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { stopIds = listOf(stop2.id, stop1.id, stop0.id) }
            }

        val globalData = GlobalResponse(objects)

        assertEquals(
            RouteDetailsStopList(
                0,
                listOf(
                    RouteDetailsStopList.OldSegment(
                        listOf(
                            RouteDetailsStopList.OldEntry(
                                stop0,
                                connectingRoutes = listOf(),
                                patterns = listOf(patternTypical0),
                            ),
                            RouteDetailsStopList.OldEntry(
                                stop1,
                                connectingRoutes = listOf(),
                                patterns = listOf(patternTypical0),
                            ),
                            RouteDetailsStopList.OldEntry(
                                stop2,
                                connectingRoutes = listOf(),
                                patterns = listOf(patternTypical0),
                            ),
                        ),
                        hasRouteLine = true,
                    )
                ),
                null,
            ),
            RouteDetailsStopList.fromOldPieces(
                mainRoute.id,
                0,
                OldRouteStopsResult(mainRoute.id, 0, listOf(stop0.id, stop1.id, stop2.id)),
                globalData,
            ),
        )
    }
}
