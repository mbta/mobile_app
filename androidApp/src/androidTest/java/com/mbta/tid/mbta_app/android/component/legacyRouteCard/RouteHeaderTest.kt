package com.mbta.tid.mbta_app.android.component.legacyRouteCard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import org.junit.Rule
import org.junit.Test

class RouteHeaderTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testRouteHeaderForBus() {
        composeTestRule.setContent {
            RouteHeader(
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
            )
        }
        composeTestRule.onNodeWithText("Short name").assertIsDisplayed()
    }

    @Test
    fun testRouteHeaderForFerry() {
        composeTestRule.setContent {
            RouteHeader(
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
            )
        }
        composeTestRule.onNodeWithText("Long name").assertIsDisplayed()
    }

    @Test
    fun testRouteHeaderForCommuterRail() {
        composeTestRule.setContent {
            RouteHeader(
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
            )
        }
        composeTestRule.onNodeWithText("Long name").assertIsDisplayed()
    }

    @Test
    fun testRouteHeaderForSubway() {
        composeTestRule.setContent {
            RouteHeader(
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
            )
        }
        composeTestRule.onNodeWithText("Long name").assertIsDisplayed()
    }
}
