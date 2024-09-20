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
                RealtimePatterns.ByHeadsign(
                    route,
                    "A",
                    null,
                    listOf(aPattern),
                    emptyList(),
                    emptyList(),
                    true
                ),
                RealtimePatterns.ByHeadsign(
                    route,
                    "B",
                    null,
                    listOf(bPattern),
                    emptyList(),
                    emptyList(),
                    true
                ),
            )
        val patternsByStop =
            PatternsByStop(
                listOf(route),
                null,
                stop,
                patterns,
                listOf(Direction("A", null, 0), Direction("B", null, 1))
            )
        var filter: StopDetailsFilter? = StopDetailsFilter(routeId = route.id, directionId = 0)
        composeTestRule.setContent {
            DirectionPicker(patternsByStop = patternsByStop, filter = filter) { newFilter ->
                filter = newFilter
            }
        }

        composeTestRule.onNodeWithText("A").assertIsDisplayed()
        composeTestRule.onNodeWithText("B").performClick()
        assertTrue(filter?.routeId == route.id)
        assertTrue(filter?.directionId == 1)
    }
}
