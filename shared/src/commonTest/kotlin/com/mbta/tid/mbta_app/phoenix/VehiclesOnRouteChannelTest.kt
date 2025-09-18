package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Month
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
                            put("current_stop_sequence", 30)
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
                            30,
                            0,
                            42.359901428222656,
                            -71.09449005126953,
                            EasternTimeInstant(2024, Month.MAY, 15, 9, 0),
                            "1",
                            "99",
                            "61391720",
                        )
                )
            ),
            parsed,
        )
    }

    @Test
    fun testTopicInterpolation() {
        val topic1 = VehiclesOnRouteChannel(listOf("Red"), 0).topic
        val topic2 = VehiclesOnRouteChannel(listOf("Green-B", "Green-C", "Green-D"), 1).topic

        assertEquals("vehicles:routes:Red:0", topic1)
        assertEquals("vehicles:routes:Green-B,Green-C,Green-D:1", topic2)
    }
}
