package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.mbta.tid.mbta_app.android.testUtils.assertCanBeDisplayed
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import org.junit.Rule
import org.junit.Test

class RoutePillTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testBusTypeText() {
        val busRoute =
            ObjectCollectionBuilder().route {
                type = RouteType.BUS
                shortName = "Harvard"
            }

        composeTestRule.setContent { RoutePill(busRoute, null, RoutePillType.Fixed) }
        composeTestRule.onNodeWithContentDescription("Harvard bus").assertCanBeDisplayed()
    }

    @Test
    fun testRailTypeText() {
        val subwayRoute =
            ObjectCollectionBuilder().route {
                type = RouteType.HEAVY_RAIL
                longName = "Red Line"
            }

        composeTestRule.setContent { RoutePill(subwayRoute, null, RoutePillType.Fixed) }
        composeTestRule.onNodeWithContentDescription("Red Line train").assertCanBeDisplayed()
    }

    @Test
    fun testCRTypeText() {
        val crRoute =
            ObjectCollectionBuilder().route {
                type = RouteType.COMMUTER_RAIL
                longName = "Haverhill"
            }

        composeTestRule.setContent { RoutePill(crRoute, null, RoutePillType.Fixed) }
        composeTestRule.onNodeWithContentDescription("Haverhill train").assertCanBeDisplayed()
    }

    @Test
    fun testFerryTypeText() {
        val ferryRoute =
            ObjectCollectionBuilder().route {
                type = RouteType.FERRY
                longName = "Charlestown Ferry"
            }

        composeTestRule.setContent { RoutePill(ferryRoute, null, RoutePillType.Fixed) }
        composeTestRule.onNodeWithContentDescription("Charlestown Ferry").assertCanBeDisplayed()
    }

    @Test
    fun testWarningIconIncluded() {
        val route =
            ObjectCollectionBuilder().route {
                type = RouteType.BUS
                shortName = "Harvard"
            }

        composeTestRule.setContent {
            RoutePill(
                route,
                null,
                RoutePillType.Fixed,
                warningAlertIconName = "alert-large-bus-issue",
            )
        }
        composeTestRule.onNodeWithContentDescription("Harvard bus").assertCanBeDisplayed()
        composeTestRule.onNodeWithContentDescription("Alert").assertCanBeDisplayed()
    }
}
