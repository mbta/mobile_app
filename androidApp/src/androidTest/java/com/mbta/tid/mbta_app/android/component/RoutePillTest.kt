package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
        composeTestRule.onNodeWithContentDescription("Harvard bus").assertIsDisplayed()
    }

    @Test
    fun testRailTypeText() {
        val subwayRoute =
            ObjectCollectionBuilder().route {
                type = RouteType.HEAVY_RAIL
                longName = "Red Line"
            }

        composeTestRule.setContent { RoutePill(subwayRoute, null, RoutePillType.Fixed) }
        composeTestRule.onNodeWithContentDescription("Red Line train").assertIsDisplayed()
    }

    @Test
    fun testCRTypeText() {
        val crRoute =
            ObjectCollectionBuilder().route {
                type = RouteType.COMMUTER_RAIL
                longName = "Haverhill"
            }

        composeTestRule.setContent { RoutePill(crRoute, null, RoutePillType.Fixed) }
        composeTestRule.onNodeWithContentDescription("Haverhill train").assertIsDisplayed()
    }

    @Test
    fun testFerryTypeText() {
        val ferryRoute =
            ObjectCollectionBuilder().route {
                type = RouteType.FERRY
                longName = "Charlestown Ferry"
            }

        composeTestRule.setContent { RoutePill(ferryRoute, null, RoutePillType.Fixed) }
        composeTestRule.onNodeWithContentDescription("Charlestown Ferry").assertIsDisplayed()
    }
}
