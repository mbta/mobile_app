package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.mocks.MockMessage
import com.mbta.tid.mbta_app.model.PredictionsError
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail
import org.koin.test.KoinTest

class PredictionsRepositoryTests : KoinTest {

    @Test
    fun testSocketConnectCalledOnRun() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = mock<PhoenixPush>(MockMode.autofill)
        every { channel.attach() } returns push
        every { push.receive(any(), any()) } returns push
        every { socket.getChannel(any(), any()) } returns channel
        val predictionsRepo = PredictionsRepository(socket)
        predictionsRepo.connect(stopIds = listOf("1"), onReceive = { /* no-op */})
        verify { socket.attach() }
    }

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
        predictionsRepo.connect(stopIds = listOf("1"), onReceive = { /* no-op */})
        assertNotNull(predictionsRepo.channel)
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
                callback: (PhoenixMessage) -> Unit
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
                outcome.data?.let {
                    assertEquals(
                        it,
                        PredictionsStreamDataResponse(emptyMap(), emptyMap(), emptyMap())
                    )
                }
                outcome.error?.let { fail() }
            }
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
                assertNotNull(outcome.error)
                assertEquals(outcome.error, PredictionsError.Unknown)
            }
        )
    }
}
