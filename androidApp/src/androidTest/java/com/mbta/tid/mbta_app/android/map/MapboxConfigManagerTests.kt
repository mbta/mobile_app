package com.mbta.tid.mbta_app.android.map

import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ConfigResponse
import com.mbta.tid.mbta_app.repositories.MockConfigRepository
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.usecases.ConfigUseCase
import junit.framework.TestCase.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test

class MapboxConfigManagerTests {

    @Test
    fun testWhenLoadConfigSuccessTokenSet() = runBlocking {
        var configuredToken: String? = null
        val configManager =
            MapboxConfigManager(
                ConfigUseCase(
                    MockConfigRepository(ApiResult.Ok(ConfigResponse("fake_token"))),
                    MockSentryRepository(),
                ),
                configureMapboxToken = { configuredToken = it },
            )

        configManager.loadConfig()

        assertEquals("fake_token", configuredToken)
    }

    @Test
    fun testWhenLoadConfigErrorTokenNotSet() = runBlocking {
        var configureTokenCalled = false
        val configManager =
            MapboxConfigManager(
                configUseCase =
                    ConfigUseCase(
                        MockConfigRepository(ApiResult.Error(500, "oops")),
                        MockSentryRepository(),
                    ),
                configureMapboxToken = { configureTokenCalled = true },
            )

        configManager.loadConfig()

        assertFalse { configureTokenCalled }
    }

    @Test
    fun testInterceptorSetOnInit() {
        var httpInterceptorSet = false
        val configManager =
            MapboxConfigManager(
                configureMapboxToken = {},
                setHttpInterceptor = { httpInterceptorSet = true },
            )
        assertTrue { httpInterceptorSet }
    }
}
