package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.phoenix.MockWebSocketSession
import com.mbta.tid.mbta_app.phoenix.PhoenixSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.putJsonObject

class PredictionsStopsChannelTest {
    @Test
    fun `sorts incoming predictions`() = runBlocking {
        val earlyPrediction =
            Prediction(
                id = "early",
                arrivalTime = Instant.parse("2024-02-14T11:04:12-05:00"),
                departureTime = Instant.parse("2024-02-14T11:04:30-05:00"),
                directionId = 0,
                revenue = true,
                scheduleRelationship = Prediction.ScheduleRelationship.Scheduled,
                status = null,
                stopSequence = 40,
                stopId = "1",
                tripId = "one",
                vehicleId = null
            )

        val latePrediction =
            Prediction(
                id = "late",
                arrivalTime = Instant.parse("2024-02-14T21:04:12-05:00"),
                departureTime = Instant.parse("2024-02-14T21:04:30-05:00"),
                directionId = 1,
                revenue = true,
                scheduleRelationship = Prediction.ScheduleRelationship.Scheduled,
                status = null,
                stopSequence = 90,
                stopId = "1",
                tripId = "two",
                vehicleId = null
            )

        val session = MockWebSocketSession(this) {}
        val socket = PhoenixSocket(session)
        withTimeout(1.seconds) {
            val channel = PredictionsStopsChannel(socket, listOf())
            channel.handle(
                "stream_data",
                buildJsonObject {
                    putJsonObject("predictions") {
                        put(latePrediction.id, Json.encodeToJsonElement(latePrediction))
                        put(earlyPrediction.id, Json.encodeToJsonElement(earlyPrediction))
                    }
                    putJsonObject("trips") {}
                    putJsonObject("vehicles") {}
                }
            )
            assertEquals(
                PredictionsStreamDataResponse(
                    predictions =
                        mapOf(
                            earlyPrediction.id to earlyPrediction,
                            latePrediction.id to latePrediction
                        ),
                    trips = emptyMap(),
                    vehicles = emptyMap()
                ),
                channel.predictions.first()
            )
        }
    }
}
