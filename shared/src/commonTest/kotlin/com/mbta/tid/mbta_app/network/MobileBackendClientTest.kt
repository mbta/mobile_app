package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.path
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.koin.test.KoinTest

class MobileBackendClientTest : KoinTest {

    @Test
    fun testGet() {
        val mockEngine = MockEngine { request ->
            respond(
                content =
                    ByteReadChannel(
                        """
                            {
                              "schedules": [{
                                }
                              ],
                              "trips": {}
                            }
                        """
                            .trimIndent()
                    ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        runBlocking {
            val response: HttpResponse =
                MobileBackendClient(mockEngine, AppVariant.Staging).get {
                    url { path("api/schedules") }
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("/api/schedules", response.request.url.encodedPath)
            assertEquals(AppVariant.Staging.backendHost, response.request.url.host)
        }
    }
}
