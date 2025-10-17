package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ConfigResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ConfigRepositoryTests {
    @AfterTest
    fun teardown() {
        stopKoin()
    }

    @Test
    fun testGetConfigSuccess() {
        val mockEngine = MockEngine { _ ->
            respond(
                content =
                    ByteReadChannel(
                        """
                            {
                              "mapbox_public_token": "fake_token"
                            }
                        """
                            .trimIndent()
                    ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        startKoin {
            modules(module { single { MobileBackendClient(mockEngine, AppVariant.Staging) } })
        }
        runBlocking {
            val response = ConfigRepository().getConfig()

            assertEquals(ApiResult.Ok(ConfigResponse("fake_token")), response)
        }
    }

    @Test
    fun testGetConfigApiError() {
        val mockEngine = MockEngine { _ ->
            respond(
                content =
                    ByteReadChannel(
                        """
                        {"message": "oh no"}
                          """
                            .trimIndent()
                    ),
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        startKoin {
            modules(module { single { MobileBackendClient(mockEngine, AppVariant.Staging) } })
        }
        runBlocking {
            val response = ConfigRepository().getConfig()

            assertIs<ApiResult.Error<*>>(response)
            assertEquals(401, response.code)
            assertTrue(response.message.contains("{\"message\": \"oh no\"}"))
        }
    }
}
