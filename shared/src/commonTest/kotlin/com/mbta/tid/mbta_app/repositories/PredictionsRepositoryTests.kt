package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.mocks.MockMessage
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixPush
import com.mbta.tid.mbta_app.network.PhoenixPushStatus
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.PredictionsForStopsChannel
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.koin.test.KoinTest

class PredictionsRepositoryTests : KoinTest {

    @Test
    fun testChannelSetOnRun() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = mock<PhoenixPush>(MockMode.autofill)
        val predictionsRepo = PredictionsRepository(socket)
        every { channel.attach() } returns push
        every { push.receive(any(), any()) } returns push
        every { socket.getChannel(any(), any()) } returns channel
        assertNull(predictionsRepo.channel)
        predictionsRepo.connect(stopIds = listOf("1"), onReceive = { /* no-op */ })
        assertNotNull(predictionsRepo.channel)
    }

    @Test
    fun testChannelClearedBeforeJoin() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = mock<PhoenixPush>(MockMode.autofill)
        val predictionsRepo = PredictionsRepository(socket)
        every { channel.attach() } returns push
        every { push.receive(any(), any()) } returns push
        every { socket.getChannel(any(), any()) } returns channel
        predictionsRepo.connect(stopIds = listOf("Test"), onReceive = {})
        verify { predictionsRepo.disconnect() }
        verify { channel.detach() }
    }

    @Test
    fun testChannelClearedOnLeave() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val predictionsRepo = PredictionsRepository(socket)
        every { socket.getChannel(any(), any()) } returns mock<PhoenixChannel>(MockMode.autofill)
        predictionsRepo.channel =
            socket.getChannel(topic = PredictionsForStopsChannel.topic, params = emptyMap())
        assertNotNull(predictionsRepo.channel)

        predictionsRepo.disconnect()
        assertNull(predictionsRepo.channel)
    }

    @Test
    fun testSetsPredictionsOnJoinResponse() {
        class MockPush : PhoenixPush {
            override fun receive(
                status: PhoenixPushStatus,
                callback: (PhoenixMessage) -> Unit,
            ): PhoenixPush {
                if (status == PhoenixPushStatus.Ok) {
                    callback(
                        MockMessage(
                            jsonBody = "{\"predictions\": {}, \"trips\": {}, \"vehicles\": {}}"
                        )
                    )
                }
                return this
            }
        }
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val predictionsRepo = PredictionsRepository(socket)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = MockPush()
        every { socket.getChannel(any(), any()) } returns channel
        every { channel.attach() } returns push

        predictionsRepo.connect(
            stopIds = listOf("1"),
            onReceive = { outcome ->
                assertEquals(
                    PredictionsStreamDataResponse(emptyMap(), emptyMap(), emptyMap()),
                    outcome.dataOrThrow(),
                )
            },
        )
    }

    @Test
    fun testSetsErrorWhenErrorReceived() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val predictionsRepo = PredictionsRepository(socket)
        val push = mock<PhoenixPush>(MockMode.autofill)
        every { push.receive(any(), any()) } returns push
        class MockChannel : PhoenixChannel {
            override fun onEvent(event: String, callback: (PhoenixMessage) -> Unit) {
                /* no-op */
            }

            override fun onFailure(callback: (message: PhoenixMessage) -> Unit) {
                callback(MockMessage())
            }

            override fun onDetach(callback: (PhoenixMessage) -> Unit) {
                /* no-op */
            }

            override fun attach(): PhoenixPush {
                return push
            }

            override fun detach(): PhoenixPush {
                return push
            }
        }
        every { socket.getChannel(any(), any()) } returns MockChannel()
        predictionsRepo.connect(
            stopIds = listOf("1"),
            onReceive = { outcome ->
                assertIs<ApiResult.Error<*>>(outcome)
                assertEquals(outcome.message, SocketError.FAILURE)
            },
        )
    }

    @Test
    fun testV2ChannelSetOnRun() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = mock<PhoenixPush>(MockMode.autofill)
        val predictionsRepo = PredictionsRepository(socket)
        every { channel.attach() } returns push

        every { push.receive(any(), any()) } returns push
        every { socket.getChannel("predictions:stops:v2:1,2", any()) } returns channel
        assertNull(predictionsRepo.channel)
        predictionsRepo.connectV2(
            stopIds = listOf("1", "2"),
            onJoin = { /* no-op */ },
            onMessage = { /* no-op */ },
        )

        assertNotNull(predictionsRepo.channel)
    }

    @Test
    fun testV2ChannelJoinTwiceLeavesOldChannel() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = mock<PhoenixPush>(MockMode.autofill)
        val predictionsRepo = PredictionsRepository(socket)
        every { channel.attach() } returns push

        every { push.receive(any(), any()) } returns push
        every { socket.getChannel(any(), any()) } returns channel
        assertNull(predictionsRepo.channel)
        predictionsRepo.connectV2(
            stopIds = listOf("1", "2"),
            onJoin = { /* no-op */ },
            onMessage = { /* no-op */ },
        )

        assertNotNull(predictionsRepo.channel)

        predictionsRepo.connectV2(
            stopIds = listOf("3", "4"),
            onJoin = { /* no-op */ },
            onMessage = { /* no-op */ },
        )

        verify { channel.detach() }
    }

    @Test
    fun testV2ChannelClearedOnLeave() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = mock<PhoenixPush>(MockMode.autofill)
        val predictionsRepo = PredictionsRepository(socket)
        every { channel.attach() } returns push

        every { push.receive(any(), any()) } returns push
        every { socket.getChannel(any(), any()) } returns channel
        assertNull(predictionsRepo.channel)
        predictionsRepo.connectV2(
            stopIds = listOf("1", "2"),
            onJoin = { /* no-op */ },
            onMessage = { /* no-op */ },
        )

        assertNotNull(predictionsRepo.channel)

        predictionsRepo.disconnect()

        verify { channel.detach() }
    }

    @Test
    fun testV2SetsPredictionsOnJoinResponse() {
        class MockPush : PhoenixPush {
            override fun receive(
                status: PhoenixPushStatus,
                callback: (PhoenixMessage) -> Unit,
            ): PhoenixPush {
                if (status == PhoenixPushStatus.Ok) {
                    callback(
                        MockMessage(
                            jsonBody =
                                """
                                {"predictions_by_stop":
                                    {"12345":
                                        {
                                            "p_1": {
                                                "id": "p_1",
                                                "arrival_time": null,
                                                "departure_time": null,
                                                "direction_id": 0,
                                                "revenue": false,
                                                "schedule_relationship": "scheduled",
                                                "status": null,
                                                "route_id": "66",
                                                "stop_id": "12345",
                                                "trip_id": "t_1",
                                                "vehicle_id": "v_1",
                                                "stop_sequence": 38
                                            }
                                        }
                                    },
                                    "trips": {
                                        "t_1": {
                                            "id": "t_1",
                                            "direction_id": 0,
                                            "headsign": "Nubian",
                                            "route_id": "66",
                                            "route_pattern_id": "66-0-0",
                                            "shape_id": "shape_id",
                                            "stop_ids": []
                                        }
                                    },
                                    "vehicles": {
                                        "v_1": {
                                            "id": "v_1",
                                            "bearing": 351,
                                            "current_status": "in_transit_to",
                                            "current_stop_sequence": 17,
                                            "direction_id": 0,
                                            "route_id": "66",
                                            "trip_id": "t_1",
                                            "stop_id": "12345",
                                            "latitude": 42.34114183,
                                            "longitude": -71.121119039,
                                            "updated_at": "2024-09-23T11:30:26-04:00"

                                        }
                                    }
                                }
                            """
                                    .trimIndent()
                        )
                    )
                }
                return this
            }
        }
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val predictionsRepo = PredictionsRepository(socket)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = MockPush()
        every { socket.getChannel(any(), any()) } returns channel
        every { channel.attach() } returns push

        predictionsRepo.connectV2(
            stopIds = listOf("1"),
            onJoin = { outcome ->
                val data = outcome.dataOrThrow()
                assertNotNull(data)
                assertEquals(1, data.predictionsByStop.size)
                assertEquals("p_1", data.predictionsByStop["12345"]?.get("p_1")?.id)

                assertEquals(1, data.trips.size)
                assertEquals("t_1", data.trips["t_1"]?.id)

                assertEquals(1, data.vehicles.size)
                assertEquals("v_1", data.vehicles["v_1"]?.id)
            },
            onMessage = { /* no-op */ },
        )
    }

    @Test
    fun testV2HandleV2Message() {

        val message =
            MockMessage(
                jsonBody =
                    """
                                {
                                    "stop_id": "12345",
                                    "predictions":
                                        {
                                            "p_1": {
                                                "id": "p_1",
                                                "arrival_time": null,
                                                "departure_time": null,
                                                "direction_id": 0,
                                                "revenue": false,
                                                "schedule_relationship": "scheduled",
                                                "status": null,
                                                "route_id": "66",
                                                "stop_id": "12345",
                                                "trip_id": "t_1",
                                                "vehicle_id": "v_1",
                                                "stop_sequence": 38
                                            }
                                        },
                                        "trips": {
                                            "t_1": {
                                                "id": "t_1",
                                                "direction_id": 0,
                                                "headsign": "Nubian",
                                                "route_id": "66",
                                                "route_pattern_id": "66-0-0",
                                                "shape_id": "shape_id",
                                                "stop_ids": []
                                            }
                                        },
                                        "vehicles": {
                                            "v_1": {
                                                "id": "v_1",
                                                "bearing": 351,
                                                "current_status": "in_transit_to",
                                                "current_stop_sequence": 17,
                                                "direction_id": 0,
                                                "route_id": "66",
                                                "trip_id": "t_1",
                                                "stop_id": "12345",
                                                "latitude": 42.34114183,
                                                "longitude": -71.121119039,
                                                "updated_at": "2024-09-23T11:30:26-04:00"

                                            }
                                        }
                                }
                            """
                        .trimIndent()
            )
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val predictionsRepo = PredictionsRepository(socket)
        predictionsRepo.handleV2Message(
            message,
            onMessage = { outcome ->
                val data = outcome.dataOrThrow()
                assertNotNull(data)
                assertEquals("12345", data.stopId)
                assertEquals("p_1", data.predictions["p_1"]?.id)

                assertEquals(1, data.trips.size)
                assertEquals("t_1", data.trips["t_1"]?.id)

                assertEquals(1, data.vehicles.size)
                assertEquals("v_1", data.vehicles["v_1"]?.id)
            },
        )
    }

    @Test
    fun testV2SetsErrorWhenReceivedOnJoin() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val predictionsRepo = PredictionsRepository(socket)
        val push = mock<PhoenixPush>(MockMode.autofill)
        every { push.receive(any(), any()) } returns push
        class MockChannel : PhoenixChannel {
            override fun onEvent(event: String, callback: (PhoenixMessage) -> Unit) {
                /* no-op */
            }

            override fun onFailure(callback: (message: PhoenixMessage) -> Unit) {
                callback(MockMessage())
            }

            override fun onDetach(callback: (PhoenixMessage) -> Unit) {
                /* no-op */
            }

            override fun attach(): PhoenixPush {
                return push
            }

            override fun detach(): PhoenixPush {
                return push
            }
        }
        every { socket.getChannel(any(), any()) } returns MockChannel()
        predictionsRepo.connectV2(
            stopIds = listOf("1"),
            onJoin = { outcome ->
                assertIs<ApiResult.Error<*>>(outcome)
                assertEquals(SocketError.RECEIVED_ERROR, outcome.message)
            },
            onMessage = { /* no-op */ },
        )
    }

    @Test
    fun testV2SetsErrorWhenReceivedOnMessage() {

        val message = MockMessage(jsonBody = "BAD_DATA")
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val predictionsRepo = PredictionsRepository(socket)
        predictionsRepo.handleV2Message(
            message,
            onMessage = { outcome ->
                assertIs<ApiResult.Error<*>>(outcome)
                assertEquals(SocketError.FAILED_TO_PARSE, outcome.message)
            },
        )
    }

    @Test
    fun `shouldForgetPredictions false when never updated`() {
        val predictionsRepo = PredictionsRepository(mock(MockMode.autofill))
        predictionsRepo.lastUpdated = null
        // there will not, in practice, be ten predictions and no last updated time
        assertFalse(predictionsRepo.shouldForgetPredictions(10))
    }

    @Test
    fun `shouldForgetPredictions false when no predictions`() {
        val predictionsRepo = PredictionsRepository(mock(MockMode.autofill))
        predictionsRepo.lastUpdated = Instant.DISTANT_PAST
        assertFalse(predictionsRepo.shouldForgetPredictions(0))
    }

    @Test
    fun `shouldForgetPredictions false when within ten minutes`() {
        val predictionsRepo = PredictionsRepository(mock(MockMode.autofill))
        predictionsRepo.lastUpdated = Clock.System.now() - 9.9.minutes
        assertFalse(predictionsRepo.shouldForgetPredictions(10))
    }

    @Test
    fun `shouldForgetPredictions true when old and nonempty`() {
        val predictionsRepo = PredictionsRepository(mock(MockMode.autofill))
        predictionsRepo.lastUpdated = Clock.System.now() - 10.1.minutes
        assertTrue(predictionsRepo.shouldForgetPredictions(10))
    }
}
