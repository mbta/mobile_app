package com.mbta.tid.mbta_app.android.search.results

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.model.RouteResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class RouteResultsViewTest {
    @get:Rule var composeTestRule = createComposeRule()

    @Test
    fun testBusBothNames() {
        val route = TestData.getRoute("87")
        composeTestRule.setContent {
            RouteResultsView(
                RoundedCornerShape(0.dp),
                RouteResult(route),
                GlobalResponse(TestData),
                handleSearch = {},
            )
        }
        composeTestRule.onNodeWithText(route.shortName).assertIsDisplayed()
        composeTestRule.onNodeWithText(route.longName).assertIsDisplayed()
    }

    @Test
    fun testTap() {
        val route = TestData.getRoute("Red")
        val taps = mutableListOf<String>()
        composeTestRule.setContent {
            RouteResultsView(
                RoundedCornerShape(0.dp),
                RouteResult(route),
                GlobalResponse(TestData),
                handleSearch = { taps.add(it) },
            )
        }
        composeTestRule.onNodeWithText("RL").performClick()
        assertEquals(listOf("Red"), taps)
        composeTestRule.onNodeWithText("Red Line").performClick()
        assertEquals(listOf("Red", "Red"), taps)
    }
}
