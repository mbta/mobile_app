package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.Trip
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
import kotlinx.serialization.json.putJsonArray

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
                trip = Trip(id = "one", routePatternId = null, stops = null)
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
                trip = Trip(id = "two", routePatternId = null, stops = null)
            )

        val session = MockWebSocketSession(this) {}
        val socket = PhoenixSocket(session)
        withTimeout(1.seconds) {
            val channel = PredictionsStopsChannel(socket, listOf())
            channel.handle(
                "stream_data",
                buildJsonObject {
                    putJsonArray("predictions") {
                        add(Json.encodeToJsonElement(latePrediction))
                        add(Json.encodeToJsonElement(earlyPrediction))
                    }
                }
            )
            assertEquals(listOf(earlyPrediction, latePrediction), channel.predictions.first())
        }
    }
}
