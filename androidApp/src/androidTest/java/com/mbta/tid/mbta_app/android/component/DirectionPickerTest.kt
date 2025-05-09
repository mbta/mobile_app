package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.stopDetails.DirectionPicker
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test

class DirectionPickerTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testDirectionToggle() {
        val objects = ObjectCollectionBuilder()
        val route =
            objects.route {
                color = "000000"
                textColor = "ffffff"
            }
        var filter: StopDetailsFilter? = StopDetailsFilter(routeId = route.id, directionId = 0)
        composeTestRule.setContent {
            DirectionPicker(
                availableDirections = listOf(0, 1),
                directions = listOf(Direction("North", null, 0), Direction("South", null, 1)),
                route = route,
                line = null,
                filter = filter,
                updateStopFilter = { newFilter -> filter = newFilter },
            )
        }

        composeTestRule.onNodeWithText("Northbound").assertIsDisplayed()
        composeTestRule.onNodeWithText("Southbound").performClick()
        assertTrue(filter?.routeId == route.id)
        assertTrue(filter?.directionId == 1)
    }

    @Test
    fun testSingleDirection() {
        val objects = ObjectCollectionBuilder()
        val route =
            objects.route {
                color = "000000"
                textColor = "ffffff"
            }
        var filter: StopDetailsFilter? = StopDetailsFilter(routeId = route.id, directionId = 1)
        composeTestRule.setContent {
            DirectionPicker(
                availableDirections = listOf(1),
                directions =
                    listOf(Direction("North", null, 0), Direction("South", "Destination", 1)),
                route = route,
                line = null,
                filter = filter,
                updateStopFilter = { newFilter -> filter = newFilter },
            )
        }

        composeTestRule.onNodeWithText("Northbound").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("Southbound to").assertIsDisplayed()
        composeTestRule.onNodeWithText("Destination").assertIsDisplayed()
    }
}
