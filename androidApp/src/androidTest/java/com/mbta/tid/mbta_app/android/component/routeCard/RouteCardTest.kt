package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import org.junit.Rule
import org.junit.Test

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
                    LineOrRoute.Route(route),
                    listOf(
                        RouteCardData.RouteStopData(
                            LineOrRoute.Route(route),
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
                showStopHeader = true,
            ) { _, _ ->
            }
        }

        composeTestRule.onNodeWithText(route.label).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop.name).assertIsDisplayed()
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

        composeTestRule.setContent {
            RouteCard(
                RouteCardData(
                    LineOrRoute.Route(route),
                    listOf(
                        RouteCardData.RouteStopData(
                            LineOrRoute.Route(route),
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
                showStopHeader = true,
            ) { _, _ ->
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
                    LineOrRoute.Route(route),
                    listOf(
                        RouteCardData.RouteStopData(
                            LineOrRoute.Route(route),
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
                showStopHeader = false,
            ) { _, _ ->
            }
        }

        composeTestRule.onNodeWithText(route.label).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop.name).assertDoesNotExist()
    }
}
