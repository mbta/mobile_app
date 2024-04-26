package com.mbta.tid.mbta_app.android.fetcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.Backend
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class BackendFetcherTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun receivesData() = runTest {
        // ensure we can check for the pending state explicitly
        val requestSync = Channel<Unit>(Channel.RENDEZVOUS)
        suspend fun awaitCanRespond() = requestSync.receive()
        suspend fun markCanRespond() = requestSync.send(Unit)

        // optionally wait for the response to have been delivered to the app
        val clientRequestPending = ManualIdlingResource()

        val engine = MockEngine { request ->
            val latitude = request.url.parameters["latitude"].orEmpty().toDouble()
            awaitCanRespond()
            respond(
                """{"stop_ids":["latitude=$latitude"]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val backend = Backend(engine)

        var key by mutableStateOf(1)
        var lastState: NearbyResponse? =
            NearbyResponse(listOf("initial response that should never be seen"))
        composeTestRule.setContent {
            lastState =
                getBackendData(backend = backend, effectKey = key) {
                    withIdlingResource(clientRequestPending) { getNearby(key.toDouble(), -1.0) }
                }
        }

        // when the first request is pending, the value should be null
        composeTestRule.awaitIdle()
        assertNull(lastState)

        // after the first response has been received, the value should match it
        markCanRespond()
        composeTestRule.awaitIdleIncluding(clientRequestPending)
        assertEquals(NearbyResponse(listOf("latitude=1.0")), lastState)

        key++

        // when the second request is pending, the old value should persist
        composeTestRule.awaitIdle()
        markCanRespond()
        assertEquals(NearbyResponse(listOf("latitude=1.0")), lastState)

        // after the second response has been received, the value should match it
        composeTestRule.awaitIdleIncluding(clientRequestPending)
        assertEquals(NearbyResponse(listOf("latitude=2.0")), lastState)
    }
}
