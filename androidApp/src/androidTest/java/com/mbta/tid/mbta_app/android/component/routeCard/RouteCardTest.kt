package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import kotlinx.datetime.Clock
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
                now,
                false
            ) { _, _ ->
            }
        }

        composeTestRule.onNodeWithText(route.label).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop.name).assertIsDisplayed()
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
                now,
                false
            ) { _, _ ->
            }
        }

        composeTestRule.onNodeWithText(route.label).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop.name).assertDoesNotExist()
    }
}
