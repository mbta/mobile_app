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
    fun `isScheduledTrackChange true when CR schedule at non-canonical stop`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.COMMUTER_RAIL }
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
