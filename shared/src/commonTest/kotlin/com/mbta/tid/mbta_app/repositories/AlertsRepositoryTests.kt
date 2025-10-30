package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.mocks.MockMessage
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.network.MobileBackendClient
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixPush
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.AlertsChannel
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Month

@OptIn(ExperimentalCoroutinesApi::class)
class AlertsRepositoryTests {
    private val emptyBackendClient =
        MobileBackendClient(MockEngine { respondBadRequest() }, AppVariant.Staging)

    @Test
    fun testChannelSetOnRun() = runTest {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = mock<PhoenixPush>(MockMode.autofill)
        val alertsRepo =
            AlertsRepository(socket, emptyBackendClient, StandardTestDispatcher(testScheduler))
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
        val alertsRepo =
            AlertsRepository(socket, emptyBackendClient, StandardTestDispatcher(testScheduler))
        every { channel.attach() } returns push
        every { push.receive(any(), any()) } returns push
        every { socket.getChannel(any(), any()) } returns channel
        alertsRepo.connect(onReceive = {})
        advanceUntilIdle()
        verify { alertsRepo.disconnect() }
        verify { channel.detach() }
    }

    @Test
    fun testChannelClearedOnLeave() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val alertsRepo = AlertsRepository(socket, emptyBackendClient, Dispatchers.IO)
        every { socket.getChannel(any(), any()) } returns mock<PhoenixChannel>(MockMode.autofill)
        alertsRepo.channel = socket.getChannel(topic = AlertsChannel.topic, params = emptyMap())
        assertNotNull(alertsRepo.channel)

        alertsRepo.disconnect()
        assertNull(alertsRepo.channel)
    }

    @Test
    fun testSetsAlertsWhenMessageReceived() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val alertsRepo = AlertsRepository(socket, emptyBackendClient, Dispatchers.IO)
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
        val alertsRepo = AlertsRepository(socket, emptyBackendClient, Dispatchers.IO)
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
                assertEquals(outcome.message, SocketError.FAILURE)
            }
        )
    }

    @Test
    fun testGetsSnapshot() = runBlocking {
        val mockEngine = MockEngine { request ->
            assertEquals("/api/alerts", request.url.fullPath)
            respond(
                """
                    {
                        "alerts": {
                            "3": {
                                "id": "3",
                                "active_period": [],
                                "description": null,
                                "effect_name": null,
                                "header": null,
                                "informed_entity": [],
                                "lifecycle": "new",
                                "severity": 5,
                                "updated_at": "2025-10-30T15:05:00-04:00"
                            }
                        }
                    }
                """
                    .trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val alertsRepo =
            AlertsRepository(
                mock(),
                MobileBackendClient(mockEngine, AppVariant.Staging),
                Dispatchers.IO,
            )
        assertEquals(
            AlertsStreamDataResponse(
                mapOf(
                    "3" to
                        Alert(
                            id = "3",
                            activePeriod = emptyList(),
                            description = null,
                            effectName = null,
                            header = null,
                            informedEntity = emptyList(),
                            lifecycle = Alert.Lifecycle.New,
                            severity = 5,
                            updatedAt = EasternTimeInstant(2025, Month.OCTOBER, 30, 15, 5),
                            facilities = null,
                        )
                )
            ),
            alertsRepo.getSnapshot().dataOrThrow(),
        )
    }
}
