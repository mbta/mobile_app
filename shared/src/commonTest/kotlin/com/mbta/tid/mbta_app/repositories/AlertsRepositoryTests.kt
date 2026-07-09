package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.mocks.MockMessage
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixPush
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.AlertsChannel
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import kotlin.test.Test
import kotlin.test.assertContains
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

@OptIn(ExperimentalCoroutinesApi::class)
class AlertsRepositoryTests {
    @Test
    fun testChannelSetOnRun() = runTest {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = mock<PhoenixPush>(MockMode.autofill)
        val debugRepo = MockDebugRepository()
        val errorBannerRepo = MockErrorBannerStateRepository()
        val alertsRepo =
            AlertsRepository(
                socket,
                debugRepo,
                errorBannerRepo,
                StandardTestDispatcher(testScheduler),
            )
        every { channel.attach() } returns push
        every { push.receive(any(), any()) } returns push
        every { socket.getChannel(any(), any()) } returns channel
        assertNull(alertsRepo.channel)
        alertsRepo.connect(onReceive = { /* no-op */ })
        advanceUntilIdle()
        assertNotNull(alertsRepo.channel)
    }

    @Test
    fun testChannelClearedBeforeJoin() = runTest {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = mock<PhoenixPush>(MockMode.autofill)
        val debugRepo = MockDebugRepository()
        val errorBannerRepo = MockErrorBannerStateRepository()
        val alertsRepo =
            AlertsRepository(
                socket,
                debugRepo,
                errorBannerRepo,
                StandardTestDispatcher(testScheduler),
            )
        every { channel.attach() } returns push
        every { push.receive(any(), any()) } returns push
        every { socket.getChannel(any(), any()) } returns channel
        alertsRepo.connect(onReceive = {})
        advanceUntilIdle()
        verify { alertsRepo.disconnect() }
        advanceUntilIdle()
        verify { channel.detach() }
    }

    @Test
    fun testChannelClearedOnLeave() = runTest {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val debugRepo = MockDebugRepository()
        val errorBannerRepo = MockErrorBannerStateRepository()
        val alertsRepo =
            AlertsRepository(
                socket,
                debugRepo,
                errorBannerRepo,
                StandardTestDispatcher(testScheduler),
            )
        every { socket.getChannel(any(), any()) } returns mock(MockMode.autofill)
        val channel = socket.getChannel(topic = AlertsChannel.topic, params = emptyMap())
        every { channel.detach() } returns mock(MockMode.autofill)

        alertsRepo.channel = channel
        assertNotNull(alertsRepo.channel)

        alertsRepo.disconnect()
        advanceUntilIdle()
        verify { channel.detach() }
        assertNull(alertsRepo.channel)
    }

    @Test
    fun testSetsAlertsWhenMessageReceived() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val debugRepo = MockDebugRepository()
        val errorBannerRepo = MockErrorBannerStateRepository()
        val alertsRepo = AlertsRepository(socket, debugRepo, errorBannerRepo, Dispatchers.IO)
        val push = mock<PhoenixPush>(MockMode.autofill)
        every { push.receive(any(), any()) } returns push
        class MockChannel : PhoenixChannel {
            override fun onEvent(event: String, callback: (PhoenixMessage) -> Unit) {
                /* no-op */
                callback(MockMessage(subject = AlertsChannel.topic, jsonBody = "{\"alerts\": {}}"))
            }

            override fun onFailure(callback: (message: PhoenixMessage) -> Unit) {
                /* no-op */
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
        alertsRepo.connect(
            onReceive = { outcome ->
                val data = outcome.dataOrThrow()
                assertNotNull(data)
                val expectedResponse = AlertsStreamDataResponse(alerts = emptyMap())
                assertEquals(expectedResponse, data)
            }
        )
    }

    @Test
    fun testSetsErrorWhenErrorReceived() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val debugRepo = MockDebugRepository()
        val errorBannerRepo = MockErrorBannerStateRepository()
        val alertsRepo = AlertsRepository(socket, debugRepo, errorBannerRepo, Dispatchers.IO)
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
        alertsRepo.connect(
            onReceive = { outcome ->
                assertIs<ApiResult.Error<*>>(outcome)
                assertNotNull(outcome.message)
                assertContains(outcome.message, SocketError.FAILURE)
            }
        )
    }
}
