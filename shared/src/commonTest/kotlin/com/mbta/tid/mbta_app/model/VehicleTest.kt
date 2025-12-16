package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.json
import kotlin.test.Test
import kotlin.test.assertNull
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put

class VehicleTest {
    @Test
    fun `discards unknown decoration`() {
        val rawData = buildJsonObject {
            put("id", "G-12345")
            put("bearing", JsonNull)
            put("current_status", "in_transit_to")
            put("current_stop_sequence", JsonNull)
            put("direction_id", 0)
            put("latitude", 1.0)
            put("longitude", 2.0)
            put("updated_at", "2025-12-05T15:52:00-04:00")
            put("route_id", JsonNull)
            put("stop_id", JsonNull)
            put("trip_id", JsonNull)
            put("decoration", "Night Train to Rigel Film Adaptation Promotional Wrap")
        }
        val vehicle: Vehicle = json.decodeFromJsonElement(rawData)
        assertNull(vehicle.decoration)
    }
}
