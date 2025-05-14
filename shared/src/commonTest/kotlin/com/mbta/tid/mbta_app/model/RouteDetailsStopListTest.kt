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
        objects.routePattern(connectingRoute) {
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { stopIds = listOf(connectingStop.id) }
        }

        val globalData = GlobalResponse(objects)

        assertEquals(
            RouteDetailsStopList(
                listOf(
                    RouteDetailsStopList.Entry(mainStop, connectingRoutes = listOf(connectingRoute))
                )
            ),
            RouteDetailsStopList.fromPieces(
                mainRoute.id,
                RouteStopsResponse(listOf(mainStop.id)),
                globalData,
            ),
        )
    }
}
