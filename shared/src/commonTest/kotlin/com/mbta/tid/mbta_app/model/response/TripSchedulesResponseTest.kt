package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.schedule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class TripSchedulesResponseTest {
    @Test
    fun `parses schedules`() {
        val schedule1 = schedule()
        val schedule2 = schedule()
        val payload = buildJsonObject {
            put("type", "schedules")
            putJsonArray("schedules") {
                add(json.encodeToJsonElement(schedule1))
                add(json.encodeToJsonElement(schedule2))
            }
        }

        assertEquals(
            TripSchedulesResponse.Schedules(listOf(schedule1, schedule2)),
            json.decodeFromJsonElement(payload)
        )
    }

    @Test
    fun `parses stop_ids`() {
        val payload = buildJsonObject {
            put("type", "stop_ids")
            putJsonArray("stop_ids") {
                add("9")
                add("10")
                add("11")
            }
        }

        assertEquals(
            TripSchedulesResponse.StopIds(listOf("9", "10", "11")),
            json.decodeFromJsonElement(payload)
        )
    }

    @Test
    fun `parses unknown`() {
        val payload = buildJsonObject { put("type", "unknown") }

        assertEquals(TripSchedulesResponse.Unknown, json.decodeFromJsonElement(payload))
    }

    @Test
    fun `can get stops from schedules`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()
        val stop2 = objects.stop()

        val tripSchedulesResponse =
            TripSchedulesResponse.Schedules(
                listOf(
                    objects.schedule { stopId = stop1.id },
                    objects.schedule { stopId = stop2.id },
                )
            )

        val globalResponse = GlobalResponse(objects, emptyMap())

        assertEquals(listOf(stop1, stop2), tripSchedulesResponse.stops(globalResponse))
    }

    @Test
    fun `can get stops from stopIds`() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()
        val stop2 = objects.stop()

        val tripSchedulesResponse = TripSchedulesResponse.StopIds(listOf(stop1.id, stop2.id))

        val globalResponse = GlobalResponse(objects, emptyMap())

        assertEquals(listOf(stop1, stop2), tripSchedulesResponse.stops(globalResponse))
    }

    @Test
    fun `can't get stops from stopIds`() {
        val tripSchedulesResponse = TripSchedulesResponse.Unknown

        val globalResponse = GlobalResponse(ObjectCollectionBuilder(), emptyMap())

        assertNull(tripSchedulesResponse.stops(globalResponse))
    }
}
