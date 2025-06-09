package com.mbta.tid.mbta_app.android.map

import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.location.MockLocationDataManager
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.android.state.SearchResultsViewModel
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ConfigResponse
import com.mbta.tid.mbta_app.repositories.MockConfigRepository
import com.mbta.tid.mbta_app.repositories.MockSearchResultRepository
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.repositories.MockVisitHistoryRepository
import com.mbta.tid.mbta_app.usecases.ConfigUseCase
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
import io.github.dellisd.spatialk.geojson.Position
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

    @Test
    fun testUpdateCenterButtonVisibilityWhenLocationKnownAndNotFollowing() = runBlocking {
        val mapViewModel =
            MapViewModel(
                configUseCase =
                    ConfigUseCase(
                        MockConfigRepository(ApiResult.Error(500, "oops")),
                        MockSentryRepository(),
                    )
            )

        assertEquals(false, mapViewModel.showRecenterButton.value)

        val searchResultsVM =
            SearchResultsViewModel(
                MockAnalytics(),
                MockSearchResultRepository(),
                VisitHistoryUsecase(MockVisitHistoryRepository()),
            )

        val locationDataManager = MockLocationDataManager()
        locationDataManager.hasPermission = true
        val viewportProvider = ViewportProvider(MapViewportState())
        viewportProvider.setIsManuallyCentering(true)

        mapViewModel.updateCenterButtonVisibility(
            MockLocationDataManager.MockLocation(Position(0.0, 0.0)),
            locationDataManager,
            searchResultsVM,
            viewportProvider,
        )

        assertEquals(true, mapViewModel.showRecenterButton.value)
    }
}
