package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ConfigResponse
import com.mbta.tid.mbta_app.model.response.ErrorDetails
import com.mbta.tid.mbta_app.repositories.MockAppCheckRepository
import com.mbta.tid.mbta_app.repositories.MockConfigRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class ConfigUsecaseTests {
    @Test
    fun testGetConfigSuccess() {

        val appCheckRepo = MockAppCheckRepository()
        val configRepo =
            MockConfigRepository(ApiResult.Ok(ConfigResponse(mapboxPublicToken = "fake_token")))

        runBlocking {
            val response =
                ConfigUseCase(appCheckRepo = appCheckRepo, configRepo = configRepo).getConfig()

            assertEquals(response, ApiResult.Ok(ConfigResponse("fake_token")))
        }
    }

    @Test
    fun testGetConfigAppCheckError() {
        val appCheckRepo = MockAppCheckRepository(ApiResult.Error(ErrorDetails(message = "oops")))
        val configRepo =
            MockConfigRepository(ApiResult.Ok(ConfigResponse(mapboxPublicToken = "fake_token")))

        runBlocking {
            val response =
                ConfigUseCase(appCheckRepo = appCheckRepo, configRepo = configRepo).getConfig()

            assertEquals(
                ApiResult.Error(ErrorDetails(message = "app check token failure oops")),
                response
            )
        }
    }
}
