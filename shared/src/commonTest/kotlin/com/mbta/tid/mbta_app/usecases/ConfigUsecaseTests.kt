package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ConfigResponse
import com.mbta.tid.mbta_app.repositories.MockConfigRepository
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class ConfigUsecaseTests {
    @Test
    fun testGetConfigSuccess() {

        val configRepo =
            MockConfigRepository(ApiResult.Ok(ConfigResponse(mapboxPublicToken = "fake_token")))
        val mockSentryRepo = MockSentryRepository()

        runBlocking {
            val response = ConfigUseCase(configRepo, mockSentryRepo).getConfig()

            assertEquals(response, ApiResult.Ok(ConfigResponse("fake_token")))
        }
    }
}
