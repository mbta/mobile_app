package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import org.junit.Rule
import org.junit.Test

class RouteCardHeaderTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testRouteCardHeaderForBus() {
        composeTestRule.setContent {
            RouteCardHeader(
                route =
                    Route(
                        "a",
                        RouteType.BUS,
                        "ffffff",
                        listOf("east"),
                        listOf("Far Away"),
                        "Long name",
                        "Short name",
                        1,
                        "000000"
                    )
            ) {
                // Empty
            }
        }
        composeTestRule.onNodeWithText("Short name").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Bus").assertIsDisplayed()
    }

    @Test
    fun testRouteCardHeaderForFerry() {
        composeTestRule.setContent {
            RouteCardHeader(
                route =
                    Route(
                        "a",
                        RouteType.FERRY,
                        "ffffff",
                        listOf("east"),
                        listOf("Far Away"),
                        "Long name",
                        "Short name",
                        1,
                        "000000"
                    )
            ) {
                // Empty
            }
        }
        composeTestRule.onNodeWithText("Long name").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Ferry").assertIsDisplayed()
    }

    @Test
    fun testRouteCardHeaderForCommuterRail() {
        composeTestRule.setContent {
            RouteCardHeader(
                route =
                    Route(
                        "a",
                        RouteType.COMMUTER_RAIL,
                        "ffffff",
                        listOf("east"),
                        listOf("Far Away"),
                        "Long name",
                        "Short name",
                        1,
                        "000000"
                    )
            ) {
                // Empty
            }
        }
        composeTestRule.onNodeWithText("Long name").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Commuter Rail").assertIsDisplayed()
    }

    @Test
    fun testRouteCardHeaderForSubway() {
        composeTestRule.setContent {
            RouteCardHeader(
                route =
                    Route(
                        "a",
                        RouteType.LIGHT_RAIL,
                        "ffffff",
                        listOf("east"),
                        listOf("Far Away"),
                        "Long name",
                        "Short name",
                        1,
                        "000000"
                    )
            ) {
                // Empty
            }
        }
        composeTestRule.onNodeWithText("Long name").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Subway").assertIsDisplayed()
    }
}
