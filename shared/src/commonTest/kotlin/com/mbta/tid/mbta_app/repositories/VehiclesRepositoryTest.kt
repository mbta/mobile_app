package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.mocks.MockMessage
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixPush
import com.mbta.tid.mbta_app.network.PhoenixPushStatus
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.VehiclesOnRouteChannel
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.test.KoinTest

@OptIn(ExperimentalCoroutinesApi::class)
class VehiclesRepositoryTest : KoinTest {

    @Test
    fun testChannelSetOnRun() = runTest {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = mock<PhoenixPush>(MockMode.autofill)
        val vehiclesRepo = VehiclesRepository(socket, StandardTestDispatcher(testScheduler))
        every { channel.attach() } returns push
        every { push.receive(any(), any()) } returns push
        every { socket.getChannel(any(), any()) } returns channel
        assertNull(vehiclesRepo.channel)
        vehiclesRepo.connect(
            routeId = Route.Id("Red"),
            directionId = 0,
            onReceive = { /* no-op */ },
        )
        advanceUntilIdle()
        assertNotNull(vehiclesRepo.channel)
    }

    @Test
    fun testChannelClearedOnLeave() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val vehiclesRepo = VehiclesRepository(socket, Dispatchers.IO)
        every { socket.getChannel(any(), any()) } returns mock<PhoenixChannel>(MockMode.autofill)
        vehiclesRepo.channel =
            socket.getChannel(
                topic = VehiclesOnRouteChannel(emptyList(), 0).topic,
                params = emptyMap(),
            )
        assertNotNull(vehiclesRepo.channel)

        vehiclesRepo.disconnect()
        assertNull(vehiclesRepo.channel)
    }

    @Test
    fun testChannelClearedBeforeJoin() = runTest {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = mock<PhoenixPush>(MockMode.autofill)
        val vehiclesRepo = VehiclesRepository(socket, StandardTestDispatcher(testScheduler))
        every { channel.attach() } returns push
        every { push.receive(any(), any()) } returns push
        every { socket.getChannel(any(), any()) } returns channel
        vehiclesRepo.channel = channel
        vehiclesRepo.connect(routeId = Route.Id("Test"), directionId = 0, onReceive = {})
        advanceUntilIdle()
        verify { channel.detach() }
    }

    @Test
    fun testSetsVehiclesOnJoinResponse() {
        class MockPush : PhoenixPush {
            override fun receive(
                status: PhoenixPushStatus,
                callback: (PhoenixMessage) -> Unit,
            ): PhoenixPush {
                if (status == PhoenixPushStatus.Ok) {
                    callback(MockMessage(jsonBody = "{\"vehicles\": {}}"))
                }
                return this
            }
        }
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val vehiclesRepo = VehiclesRepository(socket, Dispatchers.IO)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = MockPush()
        every { socket.getChannel(any(), any()) } returns channel
        every { channel.attach() } returns push

        vehiclesRepo.connect(
            routeId = Route.Id("Red"),
            directionId = 0,
            onReceive = { outcome ->
                assertEquals(VehiclesStreamDataResponse(emptyMap()), outcome.dataOrThrow())
            },
        )
    }

    @Test
    fun testSetsErrorWhenErrorReceived() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val vehiclesRepo = VehiclesRepository(socket, Dispatchers.IO)
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
            routeId = Route.Id("Red"),
            directionId = 0,
            onReceive = { outcome ->
                assertIs<ApiResult.Error<*>>(outcome)
                assertEquals(outcome.message, SocketError.FAILURE)
            },
        )
    }
}
