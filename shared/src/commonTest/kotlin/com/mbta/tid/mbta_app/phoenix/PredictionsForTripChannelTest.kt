package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.prediction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.trip
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.vehicle
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.putJsonObject

class PredictionsForTripChannelTest {
    @Test
    fun testParseNewDataMessage() {
        val prediction1 = prediction()

        val trip1 = trip()

        val vehicle1 = vehicle { currentStatus = Vehicle.CurrentStatus.StoppedAt }

        val parsed =
            PredictionsForTripChannel.parseMessage(
                json.encodeToString(
                    (buildJsonObject {
                        putJsonObject("predictions") {
                            put(prediction1.id, Json.encodeToJsonElement(prediction1))
                        }
                        putJsonObject("trips") { put(trip1.id, Json.encodeToJsonElement(trip1)) }
                        putJsonObject("vehicles") {
                            put(vehicle1.id, Json.encodeToJsonElement(vehicle1))
                        }
                    })
                )
            )
        assertEquals(
            parsed,
            PredictionsStreamDataResponse(
                predictions = mapOf(prediction1.id to prediction1),
                trips = mapOf(trip1.id to trip1),
                vehicles = mapOf(vehicle1.id to vehicle1),
            ),
        )
    }
}
