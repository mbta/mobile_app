package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.datetime.Clock
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RouteCardTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testNearbyCard() {
        val now = Clock.System.now()
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
                    listOf(RouteCardData.RouteStopData(stop, emptyList(), emptyList())),
                    RouteCardData.Context.NearbyTransit,
                    now,
                ),
                GlobalResponse(objects),
                now,
                pinned = false,
                onPin = {},
                false
            ) { _, _ ->
            }
        }

        composeTestRule.onNodeWithText(route.label).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop.name).assertIsDisplayed()
    }

    @Test
    fun testPinRoute() {
        val now = Clock.System.now()
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
                    listOf(RouteCardData.RouteStopData(stop, emptyList(), emptyList())),
                    RouteCardData.Context.NearbyTransit,
                    now,
                ),
                GlobalResponse(objects),
                now,
                pinned = false,
                onPin = { onPinCalled = true },
                false
            ) { _, _ ->
            }
        }

        composeTestRule.onNodeWithText(route.label).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop.name).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Star route").performClick()
        assertTrue(onPinCalled)
    }

    @Test
    fun testStopDetailsCard() {
        val now = Clock.System.now()
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
                    listOf(RouteCardData.RouteStopData(stop, emptyList(), emptyList())),
                    RouteCardData.Context.StopDetailsUnfiltered,
                    now,
                ),
                GlobalResponse(objects),
                now,
                pinned = false,
                onPin = {},
                false
            ) { _, _ ->
            }
        }

        composeTestRule.onNodeWithText(route.label).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop.name).assertDoesNotExist()
    }
}
