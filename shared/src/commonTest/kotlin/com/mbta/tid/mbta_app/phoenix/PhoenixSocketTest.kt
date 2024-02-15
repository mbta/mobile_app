package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.PredictionsStopsChannel
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.Trip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Instant
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.putJsonArray

class PhoenixSocketTest {
    @Test
    fun testInteractsWithSession() = runBlocking {
        val prediction =
            Prediction(
                "prediction",
                Instant.parse("2024-02-01T17:44:21Z"),
                Instant.parse("2024-02-01T17:44:38Z"),
                0,
                true,
                Prediction.ScheduleRelationship.Scheduled,
                null,
                17,
                Trip("trip", "Harvard", "Magenta-30-0", null)
            )
        val session =
            MockWebSocketSession(this) {
                expectSend(
                    joinRef = "0",
                    ref = "0",
                    topic = "predictions:stops",
                    event = "phx_join",
                    payload = buildJsonObject { putJsonArray("stop_ids") { add("place-boyls") } }
                )
                reply()
                fireReceive(
                    joinRef = "0",
                    topic = "predictions:stops",
                    event = "stream_data",
                    payload =
                        buildJsonObject {
                            put("predictions", json.encodeToJsonElement(listOf(prediction)))
                        }
                )
                expectSend(
                    joinRef = "0",
                    ref = "1",
                    topic = "predictions:stops",
                    event = "phx_leave"
                )
                reply()
                expectClientClose()
            }
        val socket = PhoenixSocket(session)
        val socketJob = async { withTimeout(1.seconds) { socket.run() } }
        withTimeout(1.seconds) {
            val channel = PredictionsStopsChannel(socket, listOf("place-boyls"))
            channel.join()
            val actualPredictions = channel.predictions.take(1).toList(mutableListOf()).first()
            assertEquals(listOf(prediction), actualPredictions)
            channel.leave()
            socket.disconnect()
        }
        socketJob.await()
    }
}
