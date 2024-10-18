package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ConfigResponse
import com.mbta.tid.mbta_app.repositories.IAppCheckRepository
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import com.mbta.tid.mbta_app.repositories.MockAppCheckRepository
import com.mbta.tid.mbta_app.repositories.MockConfigRepository
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import dev.mokkery.MockMode
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class ConfigUsecaseTests {
    @Test
    fun testGetConfigSuccess() {

        val appCheckRepo = MockAppCheckRepository()
        val configRepo =
            MockConfigRepository(ApiResult.Ok(ConfigResponse(mapboxPublicToken = "fake_token")))
        val mockSentryRepo = MockSentryRepository()

        runBlocking {
            val response = ConfigUseCase(appCheckRepo, configRepo, mockSentryRepo).getConfig()

            assertEquals(response, ApiResult.Ok(ConfigResponse("fake_token")))
        }
    }

    @Test
    fun testGetConfigAppCheckError() {
        val appCheckRepo = MockAppCheckRepository(ApiResult.Error(message = "oops"))
        val configRepo =
            MockConfigRepository(ApiResult.Ok(ConfigResponse(mapboxPublicToken = "fake_token")))
        val mockSentryRepo = mock<ISentryRepository>(MockMode.autofill)

        runBlocking {
            val response = ConfigUseCase(appCheckRepo, configRepo, mockSentryRepo).getConfig()

            assertEquals(ApiResult.Error(message = "app check token failure oops"), response)

            verify { mockSentryRepo.captureMessage("AppCheck token error null oops") }
        }
    }

    @Test
    fun testGetConfigAppCheckException() {
        val appCheckRepo = mock<IAppCheckRepository>(MockMode.autofill)
        val expectedException = IllegalArgumentException("oops")

        everySuspend { appCheckRepo.getToken() } throws expectedException
        val configRepo =
            MockConfigRepository(ApiResult.Ok(ConfigResponse(mapboxPublicToken = "fake_token")))
        val mockSentryRepo = mock<ISentryRepository>(MockMode.autofill)

        runBlocking {
            val response = ConfigUseCase(appCheckRepo, configRepo, mockSentryRepo).getConfig()

            assertEquals(ApiResult.Error(message = "oops"), response)

            verify { mockSentryRepo.captureException(expectedException) }
        }
    }
}
