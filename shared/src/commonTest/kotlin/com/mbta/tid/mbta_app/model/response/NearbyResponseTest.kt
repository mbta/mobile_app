package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.turf.measurement.offset
import org.maplibre.spatialk.units.Bearing
import org.maplibre.spatialk.units.extensions.degrees
import org.maplibre.spatialk.units.extensions.miles
import org.maplibre.spatialk.units.extensions.times

class NearbyResponseTest {
    val searchPoint = Position(latitude = 42.3513706803105, longitude = -71.06649626809957)

    fun pointAtDistance(distanceMiles: Double): Position =
        searchPoint.offset(distanceMiles.miles, Bearing.North + Random.nextDouble() * 360.degrees)

    @Test
    fun `filters stops that are redundant to closer ones based on route patterns served`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.HEAVY_RAIL }
        val patternAllStops = objects.routePattern(route)
        val patternStop3 = objects.routePattern(route) {}

        val station = objects.stop {
            position = pointAtDistance(0.01)
            childStopIds = listOf("stop1", "stop1Node")
        }

        val stop1 = objects.stop {
            id = "stop1"
            position = pointAtDistance(0.01)
            vehicleType = RouteType.HEAVY_RAIL
            parentStationId = station.id
        }

        val stop2 = objects.stop {
            position = pointAtDistance(0.02)
            vehicleType = RouteType.HEAVY_RAIL
        }

        val stop3 = objects.stop {
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

        val response = NearbyResponse(listOf(stop1.id, stop2.id, stop3.id))

        assertEquals(
            listOf(stop1.id, stop3.id),
            response.filter(
                globalData,
                AlertsStreamDataResponse(objects),
                atTime = EasternTimeInstant.now(),
            ),
        )
    }

    @Test
    fun `includes two stops if nearer has a major alert`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop { position = pointAtDistance(0.1) }
        val stop2 = objects.stop { position = pointAtDistance(0.2) }

        objects.routePattern(objects.route()) {
            representativeTrip { stopIds = listOf(stop1.id, stop2.id) }
        }

        objects.alert {
            activePeriod(EasternTimeInstant(Instant.DISTANT_PAST), end = null)
            informedEntity(
                listOf(Alert.InformedEntity.Activity.Board, Alert.InformedEntity.Activity.Exit),
                stop = stop1.id,
            )
            effect = Alert.Effect.StopClosure
        }

        val globalData = GlobalResponse(objects)
        val alerts = AlertsStreamDataResponse(objects)
        val response = NearbyResponse(listOf(stop1.id, stop2.id))

        assertEquals(
            listOf(stop1.id, stop2.id),
            response.filter(globalData, alerts, atTime = EasternTimeInstant.now()),
        )
    }

    @Test
    fun `does not include a second stop if nearer has a secondary alert`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop { position = pointAtDistance(0.1) }
        val stop2 = objects.stop { position = pointAtDistance(0.2) }

        objects.routePattern(objects.route()) {
            representativeTrip { stopIds = listOf(stop1.id, stop2.id) }
        }

        objects.alert {
            activePeriod(EasternTimeInstant(Instant.DISTANT_PAST), end = null)
            informedEntity(
                listOf(Alert.InformedEntity.Activity.Board, Alert.InformedEntity.Activity.Exit),
                stop = stop1.id,
            )
            effect = Alert.Effect.ServiceChange
        }

        val globalData = GlobalResponse(objects)
        val alerts = AlertsStreamDataResponse(objects)
        val response = NearbyResponse(listOf(stop1.id, stop2.id))

        assertEquals(
            listOf(stop1.id),
            response.filter(globalData, alerts, atTime = EasternTimeInstant.now()),
        )
    }

    @Test
    fun `skips second to include third if nearest two have a major alert`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop { position = pointAtDistance(0.1) }
        val stop2 = objects.stop { position = pointAtDistance(0.2) }
        val stop3 = objects.stop { position = pointAtDistance(0.3) }

        objects.routePattern(objects.route()) {
            representativeTrip { stopIds = listOf(stop1.id, stop2.id, stop3.id) }
        }

        objects.alert {
            activePeriod(EasternTimeInstant(Instant.DISTANT_PAST), end = null)
            informedEntity(
                listOf(Alert.InformedEntity.Activity.Board, Alert.InformedEntity.Activity.Exit),
                stop = stop1.id,
            )
            informedEntity(
                listOf(Alert.InformedEntity.Activity.Board, Alert.InformedEntity.Activity.Exit),
                stop = stop2.id,
            )
            effect = Alert.Effect.StopClosure
        }

        val globalData = GlobalResponse(objects)
        val alerts = AlertsStreamDataResponse(objects)
        val response = NearbyResponse(listOf(stop1.id, stop2.id, stop3.id))

        assertEquals(
            listOf(stop1.id, stop3.id),
            response.filter(globalData, alerts, atTime = EasternTimeInstant.now()),
        )
    }
}
