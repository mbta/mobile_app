package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext

class RouteCardTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testNearbyCard() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route =
            objects.route {
                longName = "Route"
                type = RouteType.LIGHT_RAIL
            }

        composeTestRule.setContent {
            RouteCard(
                RouteCardData(
                    RouteCardData.LineOrRoute.Route(route),
                    listOf(
                        RouteCardData.RouteStopData(
                            RouteCardData.LineOrRoute.Route(route),
                            stop,
                            emptyList(),
                            emptyList(),
                        )
                    ),
                    now,
                ),
                GlobalResponse(objects),
                now,
                isFavorite = { false },
                onPin = {},
                showStopHeader = true,
            ) { _, _ ->
            }
        }

        composeTestRule.onNodeWithText(route.label).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop.name).assertIsDisplayed()
    }

    @Test
    fun testPinRoute() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route =
            objects.route {
                longName = "Route"
                type = RouteType.LIGHT_RAIL
            }

        var onPinCalled = false

        composeTestRule.setContent {
            RouteCard(
                RouteCardData(
                    RouteCardData.LineOrRoute.Route(route),
                    listOf(
                        RouteCardData.RouteStopData(
                            RouteCardData.LineOrRoute.Route(route),
                            stop,
                            emptyList(),
                            emptyList(),
                        )
                    ),
                    now,
                ),
                GlobalResponse(objects),
                now,
                isFavorite = { false },
                onPin = { onPinCalled = true },
                showStopHeader = true,
            ) { _, _ ->
            }
        }

        composeTestRule.onNodeWithText(route.label).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop.name).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Star route").performClick()
        assertTrue(onPinCalled)
    }

    @Test
    fun testNoPinRouteButtonWhenEnhancedFavorites() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route =
            objects.route {
                longName = "Route"
                type = RouteType.LIGHT_RAIL
            }

        val koinApplication = testKoinApplication {
            settings = MockSettingsRepository(mapOf(Settings.EnhancedFavorites to true))
        }

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                RouteCard(
                    RouteCardData(
                        RouteCardData.LineOrRoute.Route(route),
                        listOf(
                            RouteCardData.RouteStopData(
                                RouteCardData.LineOrRoute.Route(route),
                                stop,
                                emptyList(),
                                emptyList(),
                            )
                        ),
                        now,
                    ),
                    GlobalResponse(objects),
                    now,
                    isFavorite = { false },
                    onPin = { false },
                    showStopHeader = true,
                ) { _, _ ->
                }
            }
        }

        composeTestRule.onNodeWithText(route.label).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop.name).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Star route").assertDoesNotExist()
    }

    @Test
    fun testStopDetailsCard() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route =
            objects.route {
                longName = "Route"
                type = RouteType.LIGHT_RAIL
            }

        composeTestRule.setContent {
            RouteCard(
                RouteCardData(
                    RouteCardData.LineOrRoute.Route(route),
                    listOf(
                        RouteCardData.RouteStopData(
                            RouteCardData.LineOrRoute.Route(route),
                            stop,
                            emptyList(),
                            emptyList(),
                        )
                    ),
                    now,
                ),
                GlobalResponse(objects),
                now,
                isFavorite = { false },
                onPin = {},
                showStopHeader = false,
            ) { _, _ ->
            }
        }

        composeTestRule.onNodeWithText(route.label).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop.name).assertDoesNotExist()
    }
}
