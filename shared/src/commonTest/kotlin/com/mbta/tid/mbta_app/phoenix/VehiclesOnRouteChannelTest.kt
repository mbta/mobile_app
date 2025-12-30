package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Month
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
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
                            put("occupancy_status", "full")
                            put("updated_at", "2024-05-15T09:00:00-04:00")
                            put("route_id", "1")
                            put("stop_id", "99")
                            put("trip_id", "61391720")
                            put("decoration", "pride")
                            putJsonArray("carriages") {
                                addJsonObject {
                                    put("occupancy_status", "no_data_available")
                                    put("occupancy_percentage", JsonNull)
                                    put("label", "1234")
                                }
                                addJsonObject {
                                    put("occupancy_status", "not_accepting_passengers")
                                    put("occupancy_percentage", 100)
                                    put("label", "4321")
                                }
                            }
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
                            listOf(
                                Vehicle.Carriage(
                                    occupancyStatus = Vehicle.OccupancyStatus.NoDataAvailable,
                                    occupancyPercentage = null,
                                    label = "1234",
                                ),
                                Vehicle.Carriage(
                                    occupancyStatus =
                                        Vehicle.OccupancyStatus.NotAcceptingPassengers,
                                    occupancyPercentage = 100,
                                    label = "4321",
                                ),
                            ),
                            Vehicle.CurrentStatus.InTransitTo,
                            30,
                            0,
                            42.359901428222656,
                            -71.09449005126953,
                            Vehicle.OccupancyStatus.Full,
                            EasternTimeInstant(2024, Month.MAY, 15, 9, 0),
                            Route.Id("1"),
                            "99",
                            "61391720",
                            Vehicle.Decoration.Pride,
                        )
                )
            ),
            parsed,
        )
    }

    @Test
    fun testTopicInterpolation() {
        val topic1 = VehiclesOnRouteChannel(listOf(Route.Id("Red")), 0).topic
        val topic2 =
            VehiclesOnRouteChannel(
                    listOf(Route.Id("Green-B"), Route.Id("Green-C"), Route.Id("Green-D")),
                    1,
                )
                .topic

        assertEquals("vehicles:routes:Red:0", topic1)
        assertEquals("vehicles:routes:Green-B,Green-C,Green-D:1", topic2)
    }
}
