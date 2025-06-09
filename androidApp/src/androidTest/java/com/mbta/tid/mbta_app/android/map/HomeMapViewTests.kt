package com.mbta.tid.mbta_app.android.map

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
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
import com.mbta.tid.mbta_app.repositories.MockConfigRepository
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.usecases.ConfigUseCase
import com.mbta.tid.mbta_app.utils.TestData
import dev.mokkery.MockMode
import dev.mokkery.answering.autofill.AutofillProvider
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
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

    @Test
    fun testRecenterButtonVisibilityCalledWhenOnStopDetails(): Unit = runBlocking {
        val locationManager = MockLocationDataManager()
        locationManager.hasPermission = true

        val viewportProvider = ViewportProvider(MapViewportState())

        var updateCenterButtonVisibilityCalled = false

        AutofillProvider.forMockMode.types.register(StateFlow::class) { MutableStateFlow(null) }

        AutofillProvider.forMockMode.types.register(StateFlow::class) { MutableStateFlow(null) }

        AutofillProvider.forMockMode.types.register(Flow::class) { MutableStateFlow(null) }

        val mapVM = mock<IMapViewModel>(MockMode.autofill)
        every { mapVM.selectedStop } returns MutableStateFlow(TestData.getStop("121"))
        every { mapVM.configLoadAttempted } returns MutableStateFlow(true)
        every { mapVM.showRecenterButton } returns MutableStateFlow(false)
        every { mapVM.showTripCenterButton } returns MutableStateFlow(false)

        every {
            mapVM.updateCenterButtonVisibility(any(), locationManager, searchVM, viewportProvider)
        } calls { updateCenterButtonVisibilityCalled = true }

        composeTestRule.setContent {
            val nearbyTransitSelectingLocationState = remember { mutableStateOf(false) }

            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastNearbyTransitLocation = null,
                nearbyTransitSelectingLocationState = nearbyTransitSelectingLocationState,
                locationDataManager = locationManager,
                viewportProvider = viewportProvider,
                currentNavEntry = SheetRoutes.StopDetails(TestData.getStop("121").id, null, null),
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                routeCardData = null,
                viewModel = mapVM,
                isSearchExpanded = false
            )
        }

        composeTestRule.waitUntil { updateCenterButtonVisibilityCalled }
        assertTrue(updateCenterButtonVisibilityCalled)
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
