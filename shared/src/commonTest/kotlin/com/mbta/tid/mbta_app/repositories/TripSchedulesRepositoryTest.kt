package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class TripSchedulesRepositoryTest : KoinTest {
    @AfterTest
    fun teardown() {
        stopKoin()
    }

    @Test
    fun `gets trip schedules`() {
        val mockEngine = MockEngine {
            respond(
                """{"type": "stop_ids", "stop_ids": ["1", "2", "3"]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        startKoin {
            modules(module { single { MobileBackendClient(mockEngine, AppVariant.Staging) } })
        }

        runBlocking {
            val response = TripSchedulesRepository().getTripSchedules(tripId = "12345")

            assertEquals(TripSchedulesResponse.StopIds(listOf("1", "2", "3")), response)
        }
    }
}
