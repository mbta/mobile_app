package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.mocks.MockMessage
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail
import org.koin.test.KoinTest

class VehiclesRepositoryTest : KoinTest {

    @Test
    fun testChannelSetOnRun() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = mock<PhoenixPush>(MockMode.autofill)
        val vehiclesRepo = VehiclesRepository(socket)
        every { channel.attach() } returns push
        every { push.receive(any(), any()) } returns push
        every { socket.getChannel(any(), any()) } returns channel
        assertNull(vehiclesRepo.channel)
        vehiclesRepo.connect(routeId = "Red", directionId = 0, onReceive = { /* no-op */})
        assertNotNull(vehiclesRepo.channel)
    }

    @Test
    fun testChannelClearedOnLeave() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val vehiclesRepo = VehiclesRepository(socket)
        every { socket.getChannel(any(), any()) } returns mock<PhoenixChannel>(MockMode.autofill)
        vehiclesRepo.channel =
            socket.getChannel(topic = PredictionsForStopsChannel.topic, params = emptyMap())
        assertNotNull(vehiclesRepo.channel)

        vehiclesRepo.disconnect()
        assertNull(vehiclesRepo.channel)
    }

    @Test
    fun testSetsVehiclesOnJoinResponse() {
        class MockPush : PhoenixPush {
            override fun receive(
                status: PhoenixPushStatus,
                callback: (PhoenixMessage) -> Unit
            ): PhoenixPush {
                if (status == PhoenixPushStatus.Ok) {
                    callback(MockMessage(jsonBody = "{\"vehicles\": {}}"))
                }
                return this
            }
        }
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val vehiclesRepo = VehiclesRepository(socket)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = MockPush()
        every { socket.getChannel(any(), any()) } returns channel
        every { channel.attach() } returns push

        vehiclesRepo.connect(
            routeId = "Red",
            directionId = 0,
            onReceive = { outcome ->
                outcome.data?.let { assertEquals(it, VehiclesStreamDataResponse(emptyMap())) }
                outcome.error?.let { fail() }
            }
        )
    }

    @Test
    fun testSetsErrorWhenErrorReceived() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val vehiclesRepo = VehiclesRepository(socket)
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
        vehiclesRepo.connect(
            routeId = "Red",
            directionId = 0,
            onReceive = { outcome ->
                assertNotNull(outcome.error)
                assertEquals(outcome.error, SocketError.Unknown)
            }
        )
    }
}
