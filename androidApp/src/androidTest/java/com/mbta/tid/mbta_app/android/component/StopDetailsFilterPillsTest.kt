package com.mbta.tid.mbta_app.android.component

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import com.mbta.tid.mbta_app.android.stopDetails.PillFilter
import com.mbta.tid.mbta_app.android.stopDetails.StopDetailsFilterPills
import com.mbta.tid.mbta_app.android.util.StopDetailsFilter
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test

class StopDetailsFilterPillsTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testFiltering() {
        val objects = ObjectCollectionBuilder()
        val route1 =
            objects.route {
                color = "000000"
                textColor = "FFFFFF"
                type = RouteType.HEAVY_RAIL
                longName = "Red Line"
            }
        val route2 =
            objects.route {
                color = "000000"
                textColor = "FFFFFF"
                type = RouteType.LIGHT_RAIL
                longName = "Mattapan Trolley"
            }
        val route3 =
            objects.route {
                color = "000000"
                textColor = "FFFFFF"
                type = RouteType.BUS
                shortName = "55"
            }

        val filter: MutableState<StopDetailsFilter?> =
            mutableStateOf(StopDetailsFilter(routeId = route1.id, directionId = 0))
        val pillRoutes =
            listOf(
                PillFilter.ByRoute(route1, null),
                PillFilter.ByRoute(route2, null),
                PillFilter.ByRoute(route3, null)
            )
        composeTestRule.setContent {
            StopDetailsFilterPills(
                servedRoutes = pillRoutes,
                filter = filter.value,
                onTapRoutePill = { pillFilter ->
                    filter.value = StopDetailsFilter(routeId = pillFilter.id, directionId = 0)
                },
                onClearFilter = { filter.value = null }
            )
        }

        composeTestRule.onNodeWithText(route1.longName, ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(route2.longName, ignoreCase = true).assertIsDisplayed()
        composeTestRule.onRoot().printToLog("ci-keep")
        Log.i("ci-keep", "------------------------")
        composeTestRule.onNodeWithText(route3.shortName).printToLog("ci-keep")
        composeTestRule.onNodeWithText(route3.shortName).assertIsDisplayed()

        composeTestRule.onNodeWithText(route2.longName, ignoreCase = true).performClick()
        assertTrue(filter.value?.routeId == route2.id)

        composeTestRule.onNodeWithText("All").performClick()
        assertTrue(filter.value == null)
        composeTestRule.onNodeWithText("All").assertDoesNotExist()

        composeTestRule.onNodeWithText(route3.shortName).performClick()
        assertTrue(filter.value?.routeId == route3.id)
    }
}
