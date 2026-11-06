package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

class TrackChangeTest {
    @Test
    fun `calculate returns matching track change alert`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route {
            id = "route-1"
            type = RouteType.COMMUTER_RAIL
        }
        val trip = objects.trip {
            id = "trip-1"
            directionId = 0
            routeId = route.id.idText
            stopIds = listOf("stop-1")
        }
        val stop = objects.stop { id = "stop-1" }
        val now = EasternTimeInstant.now()

        val alert = objects.alert {
            effect = Alert.Effect.TrackChange
            activePeriod(now - 1.minutes, now + 1.minutes)
            informedEntity(
                directionId = trip.directionId,
                route = route.id.idText,
                trip = trip.id,
                stop = stop.id,
            )
        }

        val schedule = objects.schedule {
            routeId = route.id.idText
            tripId = trip.id
            stopId = stop.id
            departureTime = now
        }

        val result =
            TrackChange.calculate(
                alerts = mapOf(alert.id to alert),
                schedule = schedule,
                prediction = null,
                trip = trip,
                stopIds = setOf(stop.id),
                globalData = GlobalResponse(objects),
            )

        assertEquals(TrackChange.Alert(alert), result)
    }

    @Test
    fun `calculate returns scheduled track change`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route {
            id = "route-1"
            type = RouteType.COMMUTER_RAIL
        }
        val canonicalTrip = objects.trip {
            id = "canonical-trip"
            directionId = 0
            routeId = route.id.idText
            stopIds = listOf("stop-1", "stop-3")
        }
        val trip = objects.trip {
            id = "trip-1"
            directionId = 0
            routeId = route.id.idText
            stopIds = listOf("stop-1", "stop-2")
        }
        val stop = objects.stop {
            id = "stop-2"
            platformCode = "2"
        }
        objects.routePattern(route) {
            id = "pattern-1"
            directionId = 0
            canonical = true
            representativeTripId = canonicalTrip.id
        }
        val schedule = objects.schedule {
            routeId = route.id.idText
            tripId = trip.id
            stopId = stop.id
            departureTime = EasternTimeInstant.now()
        }

        val result =
            TrackChange.calculate(
                alerts = emptyMap(),
                schedule = schedule,
                prediction = null,
                trip = trip,
                stopIds = setOf(stop.id),
                globalData = GlobalResponse(objects),
            )

        assertEquals(TrackChange.Scheduled("2", null), result)
    }

    @Test
    fun `calculate returns scheduled track change when prediction`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route {
            id = "route-1"
            type = RouteType.COMMUTER_RAIL
        }
        val canonicalTrip = objects.trip {
            id = "canonical-trip"
            directionId = 0
            routeId = route.id.idText
            stopIds = listOf("stop-1", "stop-3")
        }
        val trip = objects.trip {
            id = "trip-1"
            directionId = 0
            routeId = route.id.idText
            stopIds = listOf("stop-1", "stop-2")
        }
        val stop = objects.stop {
            id = "stop-2"
            platformCode = "2"
        }
        objects.routePattern(route) {
            id = "pattern-1"
            directionId = 0
            canonical = true
            representativeTripId = canonicalTrip.id
        }
        val schedule = objects.schedule {
            routeId = route.id.idText
            tripId = trip.id
            stopId = stop.id
            departureTime = EasternTimeInstant.now()
        }

        val prediction = objects.prediction {
            routeId = route.id.idText
            tripId = trip.id
            stopId = stop.id
            departureTime = EasternTimeInstant.now() + 2.minutes
        }

        val result =
            TrackChange.calculate(
                alerts = emptyMap(),
                schedule = schedule,
                prediction = prediction,
                trip = trip,
                stopIds = setOf(stop.id),
                globalData = GlobalResponse(objects),
            )

        assertEquals(TrackChange.Scheduled("2", null), result)
    }

    @Test
    fun `calculate returns alert when both alert and scheduled track change`() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route {
            id = "route-1"
            type = RouteType.COMMUTER_RAIL
        }
        val canonicalTrip = objects.trip {
            id = "canonical-trip"
            directionId = 0
            routeId = route.id.idText
            stopIds = listOf("stop-1", "stop-3")
        }
        val trip = objects.trip {
            id = "trip-1"
            directionId = 0
            routeId = route.id.idText
            stopIds = listOf("stop-1", "stop-2")
        }
        val stop = objects.stop {
            id = "stop-2"
            platformCode = "2"
        }
        objects.routePattern(route) {
            id = "pattern-1"
            directionId = 0
            canonical = true
            representativeTripId = canonicalTrip.id
        }
        val schedule = objects.schedule {
            routeId = route.id.idText
            tripId = trip.id
            stopId = stop.id
            departureTime = EasternTimeInstant.now()
        }

        val alert = objects.alert {
            effect = Alert.Effect.TrackChange
            activePeriod(now - 1.minutes, now + 1.minutes)
            informedEntity(
                directionId = trip.directionId,
                route = route.id.idText,
                trip = trip.id,
                stop = stop.id,
            )
        }

        val result =
            TrackChange.calculate(
                alerts = mapOf(alert.id to alert),
                schedule = schedule,
                prediction = null,
                trip = trip,
                stopIds = setOf(stop.id),
                globalData = GlobalResponse(objects),
            )

        assertEquals(TrackChange.Alert(alert), result)
    }

    @Test
    fun `calculate returns null when no track change applies`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route {
            id = "route-1"
            type = RouteType.BUS
        }
        val trip = objects.trip {
            id = "trip-1"
            directionId = 0
            routeId = route.id.idText
            stopIds = listOf("stop-1", "stop-2")
        }
        val stop = objects.stop { id = "stop-2" }
        val schedule = objects.schedule {
            routeId = route.id.idText
            tripId = trip.id
            stopId = stop.id
            departureTime = EasternTimeInstant.now()
        }

        val result =
            TrackChange.calculate(
                alerts = emptyMap(),
                schedule = schedule,
                prediction = null,
                trip = trip,
                stopIds = setOf(stop.id),
                globalData = GlobalResponse(objects),
            )

        assertNull(result)
    }
}
