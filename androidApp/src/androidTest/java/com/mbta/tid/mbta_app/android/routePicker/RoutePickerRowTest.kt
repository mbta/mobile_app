package com.mbta.tid.mbta_app.android.routePicker

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class RoutePickerRowTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testBasic() {
        val objects = ObjectCollectionBuilder()
        val route =
            objects.route {
                longName = "Red Line"
                type = RouteType.HEAVY_RAIL
            }

        composeTestRule.setContent { RoutePickerRow(RouteCardData.LineOrRoute.Route(route)) {} }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Red Line").assertIsDisplayed()
        composeTestRule.onNodeWithText("Red Line").assertHasClickAction()
    }

    @Test
    fun testLine() {
        val objects = TestData.clone()
        val line = objects.getLine("line-Green")
        val routes = objects.routes.values.filter { it.lineId == line.id }.toSet()

        composeTestRule.setContent {
            RoutePickerRow(RouteCardData.LineOrRoute.Line(line, routes)) {}
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Green Line").assertIsDisplayed()
    }

    @Test
    fun testTap() {
        val objects = ObjectCollectionBuilder()
        val route =
            objects.route {
                longName = "Blue Line"
                shortName = "Blue"
                type = RouteType.HEAVY_RAIL
            }

        var tapped = false
        composeTestRule.setContent {
            RoutePickerRow(RouteCardData.LineOrRoute.Route(route)) { tapped = true }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Blue Line").performClick()
        composeTestRule.waitForIdle()
        assertTrue(tapped)
    }
}
