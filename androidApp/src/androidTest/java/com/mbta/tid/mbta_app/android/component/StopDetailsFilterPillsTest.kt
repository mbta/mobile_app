package com.mbta.tid.mbta_app.android.component

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.mbta.tid.mbta_app.android.stopDetails.PillFilter
import com.mbta.tid.mbta_app.android.stopDetails.StopDetailsFilterPills
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopDetailsFilter
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
                textColor = "ffffff"
                type = RouteType.HEAVY_RAIL
                longName = "Red Line"
            }
        val route2 =
            objects.route {
                id = "Mattapan"
                color = "000000"
                textColor = "ffffff"
                type = RouteType.LIGHT_RAIL
                longName = "Mattapan Line"
            }
        val route3 =
            objects.route {
                color = "000000"
                textColor = "ffffff"
                type = RouteType.BUS
                shortName = "55"
            }

        val filter: MutableState<StopDetailsFilter?> =
            mutableStateOf(StopDetailsFilter(routeId = route1.id, directionId = 0))
        val pillRoutes =
            listOf(
                PillFilter.ByRoute(route1, null),
                PillFilter.ByRoute(route2, null),
                PillFilter.ByRoute(route3, null),
            )
        composeTestRule.setContent {
            StopDetailsFilterPills(
                servedRoutes = pillRoutes,
                filter = filter.value,
                onTapRoutePill = { pillFilter ->
                    filter.value = StopDetailsFilter(routeId = pillFilter.id, directionId = 0)
                },
                onClearFilter = { filter.value = null },
            )
        }

        composeTestRule
            .onNodeWithText("RL", ignoreCase = true)
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Red Line train")
            .assertIsSelected()
            .assert(onClickLabelContains("Remove"))

        composeTestRule
            .onNodeWithText("M", ignoreCase = true)
            .assertIsDisplayed()
            .assertIsNotSelected()
        composeTestRule
            .onNodeWithText(route3.shortName)
            .assert(onClickLabelContains("Apply"))
            .assertIsNotSelected()
            .onParent()
            .performScrollToNode(hasText(route3.shortName))

        composeTestRule
            .onNodeWithText(route3.shortName)
            .assertIsDisplayed()
            .assertContentDescriptionEquals("55 bus")
            .assertIsNotSelected()
            .assert(onClickLabelContains("Apply"))

        composeTestRule.onNodeWithText("M", ignoreCase = true).performClick()
        assertTrue(filter.value?.routeId == route2.id)

        composeTestRule.onNodeWithText("All").performClick()
        assertTrue(filter.value == null)
        composeTestRule.onNodeWithText("All").assertDoesNotExist()

        composeTestRule
            .onNodeWithText(route3.shortName)
            .assert(onClickLabelContains("Apply"))
            .performClick()
        assertTrue(filter.value?.routeId == route3.id)
    }

    fun onClickLabelContains(text: String) =
        SemanticsMatcher("hint matches") { node ->
            node.config.getOrNull(SemanticsActions.OnClick)?.label?.contains(text) ?: false
        }
}
