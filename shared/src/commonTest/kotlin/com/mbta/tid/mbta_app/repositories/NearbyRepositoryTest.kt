package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.turf.measurement.offset
import org.maplibre.spatialk.units.Bearing
import org.maplibre.spatialk.units.extensions.degrees
import org.maplibre.spatialk.units.extensions.miles
import org.maplibre.spatialk.units.extensions.times

internal class NearbyRepositoryTest {
    val searchPoint = Position(latitude = 42.3513706803105, longitude = -71.06649626809957)

    fun pointAtDistance(distanceMiles: Double): Position =
        searchPoint.offset(
            distanceMiles.miles,
            Bearing.North + Random.Default.nextDouble() * 360.degrees,
        )

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
    fun `includes nearby child stops only and not stations`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.HEAVY_RAIL }
        val patternAllStops = objects.routePattern(route)

        val station1 =
            objects.stop {
                id = "station1"
                locationType = LocationType.STATION
                position = pointAtDistance(0.01)

                childStopIds = listOf("stop1")
            }

        val stop1 =
            objects.stop {
                id = "stop1"
                position = pointAtDistance(0.01)
                vehicleType = RouteType.HEAVY_RAIL
                parentStationId = "station1"
            }

        val patternIdsByStop: Map<String, List<String>> =
            mapOf(stop1.id to listOf(patternAllStops.id))

        val globalData = GlobalResponse(objects, patternIdsByStop)

        val repo = NearbyRepository()
        val stopIdsIncludingRedundant = runBlocking {
            repo.getStopIdsNearby(
                globalData,
                Position(latitude = searchPoint.latitude, longitude = searchPoint.longitude),
            )
        }

        assertEquals(listOf(stop1.id), stopIdsIncludingRedundant)
    }

    @Test
    fun `filters stops that are redundant to closer ones based on route patterns served`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.HEAVY_RAIL }
        val patternAllStops = objects.routePattern(route)
        val patternStop3 = objects.routePattern(route) {}

        val station =
            objects.stop {
                id = "station"
                position = pointAtDistance(0.01)
                childStopIds = listOf("stop1", "stop1Node")
            }

        val stop1 =
            objects.stop {
                id = "stop1"
                position = pointAtDistance(0.01)
                vehicleType = RouteType.HEAVY_RAIL
                parentStationId = "station"
            }

        val stop2 =
            objects.stop {
                id = "stop2"
                position = pointAtDistance(0.02)
                vehicleType = RouteType.HEAVY_RAIL
            }

        val stop3 =
            objects.stop {
                id = "stop3"
                position = pointAtDistance(0.03)
                vehicleType = RouteType.HEAVY_RAIL
            }

        val patternIdsByStop: Map<String, List<String>> =
            mapOf(
                stop1.id to listOf(patternAllStops.id),
                stop2.id to listOf(patternAllStops.id),
                stop3.id to listOf(patternAllStops.id, patternStop3.id),
            )

        val globalData = GlobalResponse(objects, patternIdsByStop)

        val repo = NearbyRepository()
        val stopIdsExcludingRedundant = runBlocking {
            repo.getStopIdsNearby(
                globalData,
                Position(latitude = searchPoint.latitude, longitude = searchPoint.longitude),
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
