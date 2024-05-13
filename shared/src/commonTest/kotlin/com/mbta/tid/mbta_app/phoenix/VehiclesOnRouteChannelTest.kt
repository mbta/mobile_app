package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class VehiclesOnRouteChannelTest {
    @Test
    fun testParseNewDataMessage() {
        val payload =
            json.encodeToString(
                buildJsonObject {
                    putJsonObject("vehicles") {
                        putJsonObject("y1886") {
                            put("id", "y1886")
                            put("bearing", 315)
                            put("current_status", "in_transit_to")
                            put("direction_id", 0)
                            put("latitude", 42.359901428222656)
                            put("longitude", -71.09449005126953)
                            put("updated_at", "2024-05-15T09:00:00-04:00")
                            put("route_id", "1")
                            put("stop_id", "99")
                            put("trip_id", "61391720")
                        }
                    }
                }
            )

        val parsed = VehiclesOnRouteChannel.parseMessage(payload)

        assertEquals(
            VehiclesStreamDataResponse(
                mapOf(
                    "y1886" to
                        Vehicle(
                            "y1886",
                            315.0,
                            Vehicle.CurrentStatus.InTransitTo,
                            0,
                            42.359901428222656,
                            -71.09449005126953,
                            Instant.parse("2024-05-15T07:04:15-04:00"),
                            "1",
                            "99",
                            "61391720"
                        )
                )
            ),
            parsed
        )
    }
}
