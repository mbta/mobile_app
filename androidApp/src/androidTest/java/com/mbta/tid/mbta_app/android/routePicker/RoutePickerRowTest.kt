package com.mbta.tid.mbta_app.android.routePicker

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import org.junit.Rule
import org.junit.Test

class RoutePickerRowTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testBasic() {
        val objects = ObjectCollectionBuilder()
        val route =
            objects.route {
                shortName = "66"
                longName = "Harvard Square - Nubian Station"
                type = RouteType.BUS
            }

        composeTestRule.setContent { RoutePickerRow(LineOrRoute.Route(route)) {} }

        composeTestRule.onNodeWithText("66").assertIsDisplayed()
        composeTestRule.onNodeWithText("Harvard Square - Nubian Station").assertHasClickAction()
    }

    @Test
    fun testTap() {
        var tapped = false
        val objects = ObjectCollectionBuilder()
        val route =
            objects.route {
                longName = "Lynn Ferry"
                type = RouteType.FERRY
            }

        composeTestRule.setContent { RoutePickerRow(LineOrRoute.Route(route)) { tapped = true } }

        composeTestRule.onNodeWithText(route.longName).performClick()
        composeTestRule.waitForIdle()
        assert(tapped)
    }
}
