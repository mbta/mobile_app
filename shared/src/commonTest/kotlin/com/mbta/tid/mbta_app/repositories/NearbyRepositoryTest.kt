package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import io.github.dellisd.spatialk.geojson.Position
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import io.github.dellisd.spatialk.turf.Units
import io.github.dellisd.spatialk.turf.destination
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class NearbyRepositoryTest {
    val searchPoint = Position(latitude = 42.3513706803105, longitude = -71.06649626809957)

    @OptIn(ExperimentalTurfApi::class)
    fun pointAtDistance(distanceMiles: Double): Position =
        destination(searchPoint, distanceMiles, Random.Default.nextDouble(), Units.Miles)

    fun ObjectCollectionBuilder.rpsAtDistance(
        distanceMiles: Double,
        routeType: RouteType,
        headsign: String,
        patternIdsByStop: MutableMap<String, MutableList<String>>,
    ): Triple<Route, RoutePattern, Stop> {
        val route = this.route { type = routeType }
        val routePattern =
            this.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { this.headsign = headsign }
            }
        val stop =
            this.stop {
                position = pointAtDistance(distanceMiles)
                vehicleType = routeType
            }
        patternIdsByStop.getOrPut(stop.id, ::mutableListOf).add(routePattern.id)
        return Triple(route, routePattern, stop)
    }

    @Test
    fun `finds points within smaller radius`() {
        val objects = ObjectCollectionBuilder()
        val patternIdsByStop = mutableMapOf<String, MutableList<String>>()

        val (_, _, nearbySubwayStop) =
            objects.rpsAtDistance(0.49, RouteType.HEAVY_RAIL, "Nearby Subway", patternIdsByStop)
        objects.rpsAtDistance(0.51, RouteType.HEAVY_RAIL, "Distant Subway", patternIdsByStop)
        val (_, _, nearbyCRStop) =
            objects.rpsAtDistance(
                0.99,
                RouteType.COMMUTER_RAIL,
                "Nearby Commuter Rail",
                patternIdsByStop,
            )
        objects.rpsAtDistance(
            1.01,
            RouteType.COMMUTER_RAIL,
            "Distant Commuter Rail",
            patternIdsByStop,
        )
        val globalData = GlobalResponse(objects, patternIdsByStop)

        val repo = NearbyRepository()
        val stopIds = runBlocking {
            repo.getStopIdsNearby(
                globalData,
                Position(latitude = searchPoint.latitude, longitude = searchPoint.longitude),
            )
        }

        assertEquals(listOf(nearbySubwayStop.id, nearbyCRStop.id), stopIds)
    }

    @Test
    fun `by default includes stops with all route patterns served by closer stop`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.HEAVY_RAIL }
        val patternAllStops = objects.routePattern(route)
        val patternStop3 = objects.routePattern(route) {}

        val stop1 =
            objects.stop {
                position = pointAtDistance(0.01)
                vehicleType = RouteType.HEAVY_RAIL
            }
        val stop2 =
            objects.stop {
                position = pointAtDistance(0.02)
                vehicleType = RouteType.HEAVY_RAIL
            }

        val stop3 =
            objects.stop {
                position = pointAtDistance(0.03)
                vehicleType = RouteType.HEAVY_RAIL
            }

        val patternIdsByStop: Map<String, List<String>> =
            mapOf(
                stop1.id to listOf(patternAllStops.id),
                stop2.id to listOf(patternAllStops.id),
                stop3.id to listOf(patternAllStops.id, patternStop3.id)
            )

        val globalData = GlobalResponse(objects, patternIdsByStop)

        val repo = NearbyRepository()
        val stopIdsIncludingRedundant = runBlocking {
            repo.getStopIdsNearby(
                globalData,
                Position(latitude = searchPoint.latitude, longitude = searchPoint.longitude)
            )
        }

        assertEquals(listOf(stop1.id, stop2.id, stop3.id), stopIdsIncludingRedundant)
    }

    @Test
    fun `when excludeRedundantStops set then filters all route patterns served by closer stop`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.HEAVY_RAIL }
        val patternAllStops = objects.routePattern(route)
        val patternStop3 = objects.routePattern(route) {}

        val stop1 =
            objects.stop {
                position = pointAtDistance(0.01)
                vehicleType = RouteType.HEAVY_RAIL
            }

        val stop2 =
            objects.stop {
                position = pointAtDistance(0.02)
                vehicleType = RouteType.HEAVY_RAIL
            }

        val stop3 =
            objects.stop {
                position = pointAtDistance(0.03)
                vehicleType = RouteType.HEAVY_RAIL
            }

        val patternIdsByStop: Map<String, List<String>> =
            mapOf(
                stop1.id to listOf(patternAllStops.id),
                stop2.id to listOf(patternAllStops.id),
                stop3.id to listOf(patternAllStops.id, patternStop3.id)
            )

        val globalData = GlobalResponse(objects, patternIdsByStop)

        val repo = NearbyRepository()
        val stopIdsExcludingRedundant = runBlocking {
            repo.getStopIdsNearby(
                globalData,
                Position(latitude = searchPoint.latitude, longitude = searchPoint.longitude),
                excludeRedundantService = true
            )
        }

        assertEquals(listOf(stop1.id, stop3.id), stopIdsExcludingRedundant)
    }

    @Test
    fun `falls back to larger radius`() {
        val objects = ObjectCollectionBuilder()
        val patternIdsByStop = mutableMapOf<String, MutableList<String>>()

        val (_, _, nearbySubwayStop) =
            objects.rpsAtDistance(1.99, RouteType.HEAVY_RAIL, "Nearby Subway", patternIdsByStop)
        objects.rpsAtDistance(2.01, RouteType.HEAVY_RAIL, "Distant Subway", patternIdsByStop)
        val (_, _, nearbyCRStop) =
            objects.rpsAtDistance(
                9.99,
                RouteType.COMMUTER_RAIL,
                "Nearby Commuter Rail",
                patternIdsByStop,
            )
        objects.rpsAtDistance(
            10.01,
            RouteType.COMMUTER_RAIL,
            "Distant Commuter Rail",
            patternIdsByStop,
        )
        val globalData = GlobalResponse(objects, patternIdsByStop)

        val repo = NearbyRepository()
        val stopIds = runBlocking {
            repo.getStopIdsNearby(
                globalData,
                Position(latitude = searchPoint.latitude, longitude = searchPoint.longitude),
            )
        }

        assertEquals(listOf(nearbySubwayStop.id, nearbyCRStop.id), stopIds)
    }

    @Test
    fun `finds nothing if outside search area`() {
        val objects = ObjectCollectionBuilder()
        val patternIdsByStop = mutableMapOf<String, MutableList<String>>()

        objects.rpsAtDistance(2.01, RouteType.HEAVY_RAIL, "Distant Subway", patternIdsByStop)
        objects.rpsAtDistance(
            10.01,
            RouteType.COMMUTER_RAIL,
            "Distant Commuter Rail",
            patternIdsByStop,
        )
        val globalData = GlobalResponse(objects, patternIdsByStop)

        val repo = NearbyRepository()
        val stopIds = runBlocking {
            repo.getStopIdsNearby(
                globalData,
                Position(latitude = searchPoint.latitude, longitude = searchPoint.longitude),
            )
        }

        assertEquals(emptyList(), stopIds)
    }
}
