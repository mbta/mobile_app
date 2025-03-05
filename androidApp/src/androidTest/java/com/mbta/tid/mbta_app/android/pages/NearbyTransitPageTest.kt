package com.mbta.tid.mbta_app.android.pages

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mbta.tid.mbta_app.android.component.sheet.rememberBottomSheetScaffoldState
import com.mbta.tid.mbta_app.android.location.MockLocationDataManager
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.NearbyRepository
import com.mbta.tid.mbta_app.repositories.Settings
import org.junit.Rule
import org.junit.Test
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinContext

class NearbyTransitPageTest {
    @get:Rule val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalTestApi::class)
    @Test
    fun testHideMapsLocationUpdates() {
        val objects = ObjectCollectionBuilder()

        val stop1 =
            objects.stop {
                latitude = 1.0
                longitude = 1.0
                name = "Stop A"
                vehicleType = RouteType.BUS
            }
        val stop2 =
            objects.stop {
                latitude = 2.0
                longitude = 2.0
                name = "Stop B"
                vehicleType = RouteType.BUS
            }
        val route = objects.route { type = RouteType.BUS }
        objects.routePattern(route) {
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { stopIds = listOf(stop1.id, stop2.id) }
        }

        val alertData = AlertsStreamDataResponse(objects)
        val globalResponse = GlobalResponse(objects)

        val koin =
            testKoinApplication(objects) {
                nearby = NearbyRepository()
                settings = MockSettingsRepository(mapOf(Settings.HideMaps to true))
            }

        val locationDataManager = MockLocationDataManager(stop1.position)
        val viewportProvider = ViewportProvider(MapViewportState())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                NearbyTransitPage(
                    nearbyTransit =
                        NearbyTransit(
                            alertData = alertData,
                            globalResponse = globalResponse,
                            hideMaps = true,
                            lastNearbyTransitLocationState = remember { mutableStateOf(null) },
                            nearbyTransitSelectingLocationState =
                                remember { mutableStateOf(false) },
                            scaffoldState = rememberBottomSheetScaffoldState(),
                            locationDataManager = locationDataManager,
                            viewportProvider = viewportProvider
                        ),
                    navBarVisible = false,
                    showNavBar = {},
                    hideNavBar = {},
                    bottomBar = {},
                    searchResultsViewModel = koinViewModel()
                )
            }
        }

        composeTestRule.waitUntilExactlyOneExists(hasText(stop1.name))
        composeTestRule.onNodeWithText(stop1.name).assertIsDisplayed()

        locationDataManager.moveTo(stop2.position)

        composeTestRule.waitUntilExactlyOneExists(hasText(stop2.name))
        composeTestRule.onNodeWithText(stop2.name).assertIsDisplayed()
    }
}
