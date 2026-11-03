package com.mbta.tid.mbta_app.model.stopDetailsPage

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class TripHeaderDebugDataTest {
    private val now = EasternTimeInstant.now()
    private val objects = ObjectCollectionBuilder()
    private val trip = objects.trip {
        id = "selected-trip"
        routePatternId = "pattern"
    }

    @Test
    fun `includes vehicle data while finishing another trip`() {
        val vehicle = objects.vehicle {
            currentStatus = Vehicle.CurrentStatus.InTransitTo
            id = "selected-vehicle"
            tripId = "another-trip"
            updatedAt = now - 65.seconds
            currentStopSequence = 3
        }

        assertEquals(
            listOf(
                "trip id: selected-trip",
                "pattern id: pattern",
                "vehicle id: selected-vehicle",
                "vehicle updated 01m 05s ago",
                "current stop sequence: 3",
            ),
            TripHeaderDebugData.from(trip, "selected-vehicle", vehicle, now).lines,
        )
    }

    @Test
    fun `retains filter vehicle ID before vehicle is loaded`() {
        assertEquals(
            listOf(
                "trip id: selected-trip",
                "pattern id: pattern",
                "vehicle id: selected-vehicle",
            ),
            TripHeaderDebugData.from(trip, "selected-vehicle", null, now).lines,
        )
    }

    @Test
    fun `omits missing vehicle information`() {
        assertEquals(
            listOf("trip id: selected-trip", "pattern id: null"),
            TripHeaderDebugData.from(trip.copy(routePatternId = null), null, null, now).lines,
        )
    }

    @Test
    fun `uses loaded vehicle ID without a filter ID and omits missing sequence`() {
        val vehicle = objects.vehicle {
            currentStatus = Vehicle.CurrentStatus.InTransitTo
            id = "loaded-vehicle"
            updatedAt = now + 5.seconds
            currentStopSequence = null
        }

        assertEquals(
            listOf(
                "trip id: selected-trip",
                "pattern id: pattern",
                "vehicle id: loaded-vehicle",
                "vehicle updated 00m 05s ago",
            ),
            TripHeaderDebugData.from(trip, null, vehicle, now).lines,
        )
    }

    @Test
    fun `prefers filter ID and formats total minutes beyond an hour`() {
        val vehicle = objects.vehicle {
            currentStatus = Vehicle.CurrentStatus.InTransitTo
            id = "loaded-vehicle"
            updatedAt = now - 3661.seconds
            currentStopSequence = 0
        }

        assertEquals(
            listOf(
                "trip id: selected-trip",
                "pattern id: pattern",
                "vehicle id: selected-vehicle",
                "vehicle updated 61m 01s ago",
                "current stop sequence: 0",
            ),
            TripHeaderDebugData.from(trip, "selected-vehicle", vehicle, now).lines,
        )
    }
}
