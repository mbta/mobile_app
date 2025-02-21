package com.mbta.tid.mbta_app.android.map

import android.location.Location
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.location.MockLocationDataManager
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.android.state.SearchResultsViewModel
import com.mbta.tid.mbta_app.repositories.MockConfigRepository
import com.mbta.tid.mbta_app.repositories.MockSearchResultRepository
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.repositories.MockVisitHistoryRepository
import com.mbta.tid.mbta_app.usecases.ConfigUseCase
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class HomeMapViewTests {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testRecenterNotShownWhenNoPermissions() = runBlocking {
        val locationManager = MockLocationDataManager(Location("mock"))

        locationManager.hasPermission = false

        val viewModel =
            MapViewModel(ConfigUseCase(MockConfigRepository(), MockSentryRepository()), {})
        val viewportProvider = ViewportProvider(MapViewportState())
        viewModel.loadConfig()
        composeTestRule.setContent {
            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastNearbyTransitLocation = null,
                nearbyTransitSelectingLocationState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = viewportProvider,
                currentNavEntry = null,
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                stopDetailsDepartures = null,
                viewModel = viewModel,
                searchResultsViewModel =
                    SearchResultsViewModel(
                        MockAnalytics(),
                        MockSearchResultRepository(),
                        VisitHistoryUsecase(MockVisitHistoryRepository())
                    )
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Recenter map on my location")
            .assertDoesNotExist()
    }

    @Test
    fun testRecenterNotShownWhenPermissions(): Unit = runBlocking {
        val locationManager = MockLocationDataManager(Location("mock"))

        locationManager.hasPermission = true
        val viewModel =
            MapViewModel(ConfigUseCase(MockConfigRepository(), MockSentryRepository()), {})
        val viewportProvider = ViewportProvider(MapViewportState())
        viewModel.loadConfig()
        composeTestRule.setContent {
            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastNearbyTransitLocation = null,
                nearbyTransitSelectingLocationState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = viewportProvider,
                currentNavEntry = null,
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                stopDetailsDepartures = null,
                viewModel = viewModel,
                searchResultsViewModel =
                    SearchResultsViewModel(
                        MockAnalytics(),
                        MockSearchResultRepository(),
                        VisitHistoryUsecase(MockVisitHistoryRepository())
                    )
            )
        }
        composeTestRule
            .onNodeWithContentDescription("Recenter map on my location")
            .assertIsDisplayed()
    }

    @Test
    fun testLocationAuthShownWhenNoPermissions(): Unit = runBlocking {
        val locationManager = MockLocationDataManager(null)
        locationManager.hasPermission = false
        val viewModel =
            MapViewModel(ConfigUseCase(MockConfigRepository(), MockSentryRepository()), {})
        val viewportProvider = ViewportProvider(MapViewportState())
        viewModel.loadConfig()
        composeTestRule.setContent {
            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastNearbyTransitLocation = null,
                nearbyTransitSelectingLocationState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = viewportProvider,
                currentNavEntry = SheetRoutes.NearbyTransit,
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                stopDetailsDepartures = null,
                viewModel = viewModel,
                searchResultsViewModel =
                    SearchResultsViewModel(
                        MockAnalytics(),
                        MockSearchResultRepository(),
                        VisitHistoryUsecase(MockVisitHistoryRepository())
                    )
            )
        }
        composeTestRule.onNodeWithText("Location Services is off").assertIsDisplayed()
    }

    @Test
    fun testLocationAuthNotShownWhenPermissions() = runBlocking {
        val locationManager = MockLocationDataManager(Location("mock"))

        locationManager.hasPermission = true

        val viewModel =
            MapViewModel(ConfigUseCase(MockConfigRepository(), MockSentryRepository()), {})
        val viewportProvider = ViewportProvider(MapViewportState())
        viewModel.loadConfig()
        composeTestRule.setContent {
            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastNearbyTransitLocation = null,
                nearbyTransitSelectingLocationState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = viewportProvider,
                currentNavEntry = null,
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                stopDetailsDepartures = null,
                viewModel = viewModel,
                searchResultsViewModel =
                    SearchResultsViewModel(
                        MockAnalytics(),
                        MockSearchResultRepository(),
                        VisitHistoryUsecase(MockVisitHistoryRepository())
                    )
            )
        }

        composeTestRule.onNodeWithText("Location Services is off").assertDoesNotExist()
    }

    @Test
    fun testLocationAuthNotShownStopDetails() = runBlocking {
        val locationManager = MockLocationDataManager(Location("mock"))

        locationManager.hasPermission = false

        val viewModel =
            MapViewModel(ConfigUseCase(MockConfigRepository(), MockSentryRepository()), {})
        val viewportProvider = ViewportProvider(MapViewportState())
        viewModel.loadConfig()
        composeTestRule.setContent {
            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastNearbyTransitLocation = null,
                nearbyTransitSelectingLocationState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = viewportProvider,
                currentNavEntry = SheetRoutes.StopDetails("stopId", null, null),
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                stopDetailsDepartures = null,
                viewModel = viewModel,
                searchResultsViewModel =
                    SearchResultsViewModel(
                        MockAnalytics(),
                        MockSearchResultRepository(),
                        VisitHistoryUsecase(MockVisitHistoryRepository())
                    )
            )
        }

        composeTestRule.onNodeWithText("Location Services is off").assertDoesNotExist()
    }

    @Test
    fun testOverviewNotShownWhenNoPermissionsStopDetails() = runBlocking {
        val locationManager = MockLocationDataManager(Location("mock"))

        locationManager.hasPermission = false

        val viewModel =
            MapViewModel(ConfigUseCase(MockConfigRepository(), MockSentryRepository()), {})
        val viewportProvider = ViewportProvider(MapViewportState())
        viewModel.loadConfig()
        composeTestRule.setContent {
            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastNearbyTransitLocation = null,
                nearbyTransitSelectingLocationState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = viewportProvider,
                currentNavEntry = SheetRoutes.StopDetails("stopId", null, null),
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                stopDetailsDepartures = null,
                viewModel = viewModel,
                searchResultsViewModel =
                    SearchResultsViewModel(
                        MockAnalytics(),
                        MockSearchResultRepository(),
                        VisitHistoryUsecase(MockVisitHistoryRepository())
                    )
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Recenter map on my location")
            .assertDoesNotExist()
    }

    @Test
    fun testOverviewShownOnStopDetails(): Unit = runBlocking {
        val locationManager = MockLocationDataManager(Location("mock"))

        locationManager.hasPermission = true
        val viewModel =
            MapViewModel(ConfigUseCase(MockConfigRepository(), MockSentryRepository()), {})
        val viewportProvider = ViewportProvider(MapViewportState())
        viewModel.loadConfig()
        composeTestRule.setContent {
            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastNearbyTransitLocation = null,
                nearbyTransitSelectingLocationState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = viewportProvider,
                currentNavEntry = SheetRoutes.StopDetails("stopId", null, null),
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                stopDetailsDepartures = null,
                viewModel = viewModel,
                searchResultsViewModel =
                    SearchResultsViewModel(
                        MockAnalytics(),
                        MockSearchResultRepository(),
                        VisitHistoryUsecase(MockVisitHistoryRepository())
                    )
            )
        }
        composeTestRule.onNodeWithContentDescription("Recenter map on my location").assertExists()
    }

    @Test
    fun testPlaceholderGrid(): Unit = runBlocking {
        val viewModel =
            MapViewModel(ConfigUseCase(MockConfigRepository(), MockSentryRepository()), {})
        val viewportProvider = ViewportProvider(MapViewportState())
        composeTestRule.setContent {
            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastNearbyTransitLocation = null,
                nearbyTransitSelectingLocationState = mutableStateOf(false),
                locationDataManager = MockLocationDataManager(Location("mock")),
                viewportProvider = viewportProvider,
                currentNavEntry = null,
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                stopDetailsDepartures = null,
                viewModel = viewModel,
                searchResultsViewModel =
                    SearchResultsViewModel(
                        MockAnalytics(),
                        MockSearchResultRepository(),
                        VisitHistoryUsecase(MockVisitHistoryRepository())
                    )
            )
        }
        composeTestRule.onNodeWithTag("Empty map grid").assertIsDisplayed()
        viewModel.loadConfig()
        composeTestRule.onNodeWithTag("Empty map grid").assertIsNotDisplayed()
    }
}
