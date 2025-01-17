package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.stopDetails.DirectionPicker
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RoutePattern
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
        val stop = objects.stop()
        val aPattern =
            objects.routePattern(route) {
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val bPattern =
            objects.routePattern(route) {
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
            }
        val patterns =
            listOf(
                RealtimePatterns.ByHeadsign(route, "A", null, listOf(aPattern), emptyList()),
                RealtimePatterns.ByHeadsign(route, "B", null, listOf(bPattern), emptyList()),
            )
        val patternsByStop =
            PatternsByStop(
                listOf(route),
                null,
                stop,
                patterns,
                listOf(Direction("North", null, 0), Direction("South", null, 1)),
                emptyList()
            )
        var filter: StopDetailsFilter? = StopDetailsFilter(routeId = route.id, directionId = 0)
        composeTestRule.setContent {
            DirectionPicker(patternsByStop = patternsByStop, filter = filter) { newFilter ->
                filter = newFilter
            }
        }

        composeTestRule.onNodeWithText("Northbound").assertIsDisplayed()
        composeTestRule.onNodeWithText("Southbound").performClick()
        assertTrue(filter?.routeId == route.id)
        assertTrue(filter?.directionId == 1)
    }
}
