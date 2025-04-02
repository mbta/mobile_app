package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.response.ApiResult
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
        patternIdsByStop: MutableMap<String, MutableList<String>>
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
                patternIdsByStop
            )
        objects.rpsAtDistance(
            1.01,
            RouteType.COMMUTER_RAIL,
            "Distant Commuter Rail",
            patternIdsByStop
        )
        val globalData = GlobalResponse(objects, patternIdsByStop)

        val repo = NearbyRepository()
        val stopIds = runBlocking {
            repo.getStopIdsNearby(
                globalData,
                Position(latitude = searchPoint.latitude, longitude = searchPoint.longitude)
            )
        }

        assertEquals(listOf(nearbySubwayStop.id, nearbyCRStop.id), stopIds)
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
                patternIdsByStop
            )
        objects.rpsAtDistance(
            10.01,
            RouteType.COMMUTER_RAIL,
            "Distant Commuter Rail",
            patternIdsByStop
        )
        val globalData = GlobalResponse(objects, patternIdsByStop)

        val repo = NearbyRepository()
        val stopIds = runBlocking {
            repo.getStopIdsNearby(
                globalData,
                Position(latitude = searchPoint.latitude, longitude = searchPoint.longitude)
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
            patternIdsByStop
        )
        val globalData = GlobalResponse(objects, patternIdsByStop)

        val repo = NearbyRepository()
        val stopIds = runBlocking {
            repo.getStopIdsNearby(
                globalData,
                Position(latitude = searchPoint.latitude, longitude = searchPoint.longitude)
            )
        }

        assertEquals(emptyList(), stopIds)
    }

    @Test
    fun `getNearby returns static data`() {
        val objects = ObjectCollectionBuilder()
        val patternIdsByStop = mutableMapOf<String, MutableList<String>>()

        val (nearbySubwayRoute, nearbySubwayRoutePattern, nearbySubwayStop) =
            objects.rpsAtDistance(0.49, RouteType.HEAVY_RAIL, "Nearby Subway", patternIdsByStop)
        objects.rpsAtDistance(0.51, RouteType.HEAVY_RAIL, "Distant Subway", patternIdsByStop)
        val (nearbyCRRoute, nearbyCRRoutePattern, nearbyCRStop) =
            objects.rpsAtDistance(
                0.99,
                RouteType.COMMUTER_RAIL,
                "Nearby Commuter Rail",
                patternIdsByStop
            )
        objects.rpsAtDistance(
            1.01,
            RouteType.COMMUTER_RAIL,
            "Distant Commuter Rail",
            patternIdsByStop
        )
        val globalData = GlobalResponse(objects, patternIdsByStop)

        val repo = NearbyRepository()
        val staticData = runBlocking {
            repo.getNearby(
                globalData,
                Position(latitude = searchPoint.latitude, longitude = searchPoint.longitude)
            )
        }

        assertEquals(
            ApiResult.Ok(
                NearbyStaticData.build {
                    route(nearbySubwayRoute) {
                        stop(nearbySubwayStop) {
                            headsign("Nearby Subway", listOf(nearbySubwayRoutePattern))
                        }
                    }
                    route(nearbyCRRoute) {
                        stop(nearbyCRStop) {
                            headsign("Nearby Commuter Rail", listOf(nearbyCRRoutePattern))
                        }
                    }
                }
            ),
            staticData
        )
    }
}
