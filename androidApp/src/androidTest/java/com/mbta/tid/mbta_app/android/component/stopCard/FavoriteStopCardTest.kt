package com.mbta.tid.mbta_app.android.component.stopCard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.utils.TestData
import org.junit.Rule
import org.junit.Test

class FavoriteStopCardTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testContainsAllContent() {
        val objects = TestData.clone()
        val ol = objects.getRoute("Orange")
        val wellington = objects.getStop("place-welln")
        var toggled = false
        composeTestRule.setContent {
            FavoriteStopCard(
                stop = wellington,
                route = LineOrRoute.Route(ol),
                direction = Direction(0, ol, wellington),
                toggleDirection = { toggled = true },
            )
        }

        composeTestRule.onNodeWithText(wellington.name).assertIsDisplayed()
        composeTestRule.onNodeWithText("OL").assertIsDisplayed()
        composeTestRule.onNodeWithText("Southbound to").assertIsDisplayed()
        composeTestRule.onNodeWithText("Forest Hills").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("toggle direction")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        assert(toggled)
    }

    @Test
    fun testHidesToggleButton() {
        val objects = TestData.clone()
        val ol = objects.getRoute("Orange")
        val wellington = objects.getStop("place-welln")
        composeTestRule.setContent {
            FavoriteStopCard(
                stop = wellington,
                route = LineOrRoute.Route(ol),
                direction = Direction(0, ol, wellington),
                toggleDirection = null,
            )
        }

        composeTestRule.onNodeWithContentDescription("toggle direction").assertDoesNotExist()
    }

    @Test
    fun testOnlyOppositeDirection() {
        val objects = TestData.clone()
        val ol = objects.getRoute("Orange")
        val wellington = objects.getStop("place-welln")
        composeTestRule.setContent {
            FavoriteStopCard(
                stop = wellington,
                route = LineOrRoute.Route(ol),
                direction = Direction(0, ol, wellington),
                toggleDirection = {},
                onlyServingOppositeDirection = true,
            )
        }

        composeTestRule.onNodeWithText("Only Southbound to").assertIsDisplayed()
    }
}
