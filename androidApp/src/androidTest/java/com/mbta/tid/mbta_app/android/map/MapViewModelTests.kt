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

class MapViewModelTests {

    @Test
    fun testWhenLoadConfigSuccessTokenSet() = runBlocking {
        var configuredToken: String? = null
        val mapViewModel =
            MapViewModel(
                configUseCase =
                    ConfigUseCase(
                        MockConfigRepository(ApiResult.Ok(ConfigResponse("fake_token"))),
                        MockSentryRepository(),
                    ),
                configureMapboxToken = { token -> configuredToken = token },
                setHttpInterceptor = {},
            )

        mapViewModel.loadConfig()

        assertEquals("fake_token", configuredToken)
    }

    @Test
    fun testWhenLoadConfigErrorTokenNotSet() = runBlocking {
        var configureTokenCalled = false
        val mapViewModel =
            MapViewModel(
                configUseCase =
                    ConfigUseCase(
                        MockConfigRepository(ApiResult.Error(500, "oops")),
                        MockSentryRepository(),
                    ),
                configureMapboxToken = { configureTokenCalled = true },
                setHttpInterceptor = {},
            )

        mapViewModel.loadConfig()

        assertFalse { configureTokenCalled }
    }

    @Test
    fun testInterceptorSetOnInit() {
        var httpInterceptorSet = false
        val mapViewModel =
            MapViewModel(
                configureMapboxToken = {},
                setHttpInterceptor = { httpInterceptorSet = true },
            )
        assertTrue { httpInterceptorSet }
    }
}
