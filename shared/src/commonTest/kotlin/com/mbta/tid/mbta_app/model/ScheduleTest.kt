package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ScheduleTest {
    @Test
    fun `isScheduledTrackChange false when CR schedule at canonical stop`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.COMMUTER_RAIL }
        objects.stop { id = "canonical1" }
        objects.stop { id = "canonical2" }
        val trip = objects.trip {
            directionId = 0
            routeId = route.id.idText
            stopIds = listOf("canonical1", "canonical2")
        }
        objects.routePattern(route) {
            directionId = 0
            canonical = true
            representativeTripId = trip.id
        }

        val schedule = objects.schedule {
            routeId = route.id.idText
            stopId = "canonical1"
            tripId = trip.id
        }

        assertFalse(schedule.isScheduledTrackChange(trip, GlobalResponse(objects)))
    }

    @Test
    fun `isScheduledTrackChange false when canonical stop doesn't have platform`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.COMMUTER_RAIL }
        objects.stop {
            id = "parent"
            locationType = LocationType.STATION
            childStopIds = listOf("canonical1", "nonCanonical")
        }
        objects.stop {
            id = "canonical1"
            parentStationId = "parent"
            platformCode = null
        }
        objects.stop { id = "canonical2" }
        objects.stop {
            id = "nonCanonical"
            parentStationId = "parent"
        }
        val trip = objects.trip {
            directionId = 0
            routeId = route.id.idText
            stopIds = listOf("canonical1", "canonical2")
        }
        objects.routePattern(route) {
            directionId = 0
            canonical = true
            representativeTripId = trip.id
        }

        val schedule = objects.schedule {
            routeId = route.id.idText
            stopId = "nonCanonical"
            tripId = "otherTrip"
        }

        assertFalse(schedule.isScheduledTrackChange(trip, GlobalResponse(objects)))
    }

    @Test
    fun `isScheduledTrackChange true when CR schedule at non-canonical stop`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.COMMUTER_RAIL }
        objects.stop { id = "canonical1" }
        objects.stop { id = "canonical2" }
        objects.stop { id = "notCanonical" }
        val trip = objects.trip {
            directionId = 0
            routeId = route.id.idText
            stopIds = listOf("canonical1", "canonical2")
        }
        objects.routePattern(route) {
            directionId = 0
            canonical = true
            representativeTripId = trip.id
        }

        val schedule = objects.schedule {
            routeId = route.id.idText
            stopId = "notCanonical"
            tripId = trip.id
        }

        assertTrue(schedule.isScheduledTrackChange(trip, GlobalResponse(objects)))
    }

    @Test
    fun `isScheduledTrackChange false for bus`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.BUS }
        objects.stop { id = "canonical1" }
        objects.stop { id = "canonical2" }
        objects.stop { id = "notCanonical" }
        val trip = objects.trip {
            directionId = 0
            routeId = route.id.idText
            stopIds = listOf("canonical1", "canonical2")
        }
        objects.routePattern(route) {
            directionId = 0
            canonical = true
            representativeTripId = trip.id
        }

        val schedule = objects.schedule {
            routeId = route.id.idText
            stopId = "notCanonical"
            tripId = trip.id
        }

        assertFalse(schedule.isScheduledTrackChange(trip, GlobalResponse(objects)))
    }
}
