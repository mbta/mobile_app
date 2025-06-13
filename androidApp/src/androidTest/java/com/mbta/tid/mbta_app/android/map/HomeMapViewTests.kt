package com.mbta.tid.mbta_app.android.map

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraState
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.location.MockLocationDataManager
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.MockConfigRepository
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.usecases.ConfigUseCase
import com.mbta.tid.mbta_app.utils.TestData
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class HomeMapViewTests {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testRecenterNotShownWhenNoPermissions() = runBlocking {
        val locationManager = MockLocationDataManager()

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
                routeCardData = null,
                viewModel = viewModel,
                isSearchExpanded = false,
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Recenter map on my location")
            .assertDoesNotExist()
    }

    @Test
    fun testRecenterNotShownWhenPermissionsAndAtDefaultCenter(): Unit = runBlocking {
        val locationManager = MockLocationDataManager()

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
                routeCardData = null,
                viewModel = viewModel,
                isSearchExpanded = false,
            )
        }
        composeTestRule
            .onNodeWithContentDescription("Recenter map on my location")
            .assertIsNotDisplayed()
    }

    @Test
    fun testRecenterShownWhenPermissionsAndMapNotAtDefaultCenter(): Unit = runBlocking {
        val locationManager = MockLocationDataManager()
        val customLocation =
            Point.fromLngLat(
                ViewportProvider.Companion.Defaults.center.latitude() + 1,
                ViewportProvider.Companion.Defaults.center.longitude() + 1,
            )

        locationManager.hasPermission = true
        val viewModel =
            MapViewModel(ConfigUseCase(MockConfigRepository(), MockSentryRepository()), {})
        val viewportProvider =
            ViewportProvider(
                MapViewportState(
                    initialCameraState =
                        CameraState(customLocation, EdgeInsets(0.0, 0.0, 0.0, 0.0), 0.0, 0.0, 0.0)
                )
            )
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
                routeCardData = null,
                viewModel = viewModel,
                isSearchExpanded = false,
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
                routeCardData = null,
                viewModel = viewModel,
                isSearchExpanded = false,
            )
        }
        composeTestRule.onNodeWithText("Location Services is off").assertIsDisplayed()
    }

    @Test
    fun testLocationAuthNotShownWhenPermissions() = runBlocking {
        val locationManager = MockLocationDataManager()

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
                routeCardData = null,
                viewModel = viewModel,
                isSearchExpanded = false,
            )
        }

        composeTestRule.onNodeWithText("Location Services is off").assertDoesNotExist()
    }

    @Test
    fun testLocationAuthNotShownStopDetails() = runBlocking {
        val locationManager = MockLocationDataManager()

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
                routeCardData = null,
                viewModel = viewModel,
                isSearchExpanded = false,
            )
        }

        composeTestRule.onNodeWithText("Location Services is off").assertDoesNotExist()
    }

    @Test
    fun testOverviewNotShownWhenNoPermissionsStopDetails() = runBlocking {
        val locationManager = MockLocationDataManager()

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
                routeCardData = null,
                viewModel = viewModel,
                isSearchExpanded = false,
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Recenter map on my location")
            .assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    @Ignore("flaky test passing locally but failing in CI")
    fun testOverviewShownOnStopDetails(): Unit = runBlocking {
        val locationManager = MockLocationDataManager()
        locationManager.hasPermission = true
        val viewModel =
            MapViewModel(ConfigUseCase(MockConfigRepository(), MockSentryRepository()), {})

        viewModel.setGlobalResponse(GlobalResponse(objects = TestData))
        val viewportProvider = ViewportProvider(MapViewportState())
        viewModel.loadConfig()
        composeTestRule.setContent {
            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastNearbyTransitLocation = null,
                nearbyTransitSelectingLocationState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = viewportProvider,
                currentNavEntry = SheetRoutes.StopDetails(TestData.getStop("121").id, null, null),
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                routeCardData = null,
                viewModel = viewModel,
                isSearchExpanded = false,
            )
        }
        composeTestRule.waitUntilExactlyOneExists(
            hasContentDescription("Recenter map on my location"),
            timeoutMillis = 15000L,
        )
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
                locationDataManager = MockLocationDataManager(),
                viewportProvider = viewportProvider,
                currentNavEntry = null,
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                routeCardData = null,
                viewModel = viewModel,
                isSearchExpanded = false,
            )
        }
        composeTestRule.onNodeWithTag("Empty map grid").assertIsDisplayed()
        viewModel.loadConfig()
        composeTestRule.onNodeWithTag("Empty map grid").assertIsNotDisplayed()
    }
}
