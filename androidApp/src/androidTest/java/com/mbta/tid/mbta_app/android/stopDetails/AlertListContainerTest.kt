package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.RouteType
import org.junit.Rule
import org.junit.Test

class AlertListContainerTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val routeAccents =
        TripRouteAccents(Color.fromHex("ED8B00"), Color.fromHex("FFFFFF"), RouteType.BUS)

    @Test
    fun testShowsAllPassedViews() {

        composeTestRule.setContent {
            AlertListContainer(
                highPriority = listOf({ _ -> Text("High 1") }),
                middleContent = { _ -> Text("Middle") },
                lowPriority = listOf({ _ -> Text("Low 1") }),
            )
        }

        composeTestRule.onNodeWithText("High 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Middle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Low 1").assertIsDisplayed()
    }
}
