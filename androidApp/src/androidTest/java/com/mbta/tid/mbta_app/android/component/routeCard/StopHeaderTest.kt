package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus
import org.junit.Rule
import org.junit.Test

class StopHeaderTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testBasic() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}

        composeTestRule.setContent {
            StopHeader(RouteCardData.RouteStopData(stop, emptyList(), emptyList()), false)
        }
        composeTestRule.onNodeWithText(stop.name).assertIsDisplayed()
    }

    @Test
    fun testAccessible() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE }

        composeTestRule.setContent {
            StopHeader(RouteCardData.RouteStopData(stop, emptyList(), emptyList()), true)
        }
        composeTestRule.onNodeWithText(stop.name).assertIsDisplayed()
        composeTestRule.onNodeWithTag("wheelchair_accessible").assertIsDisplayed()
    }

    @Test
    fun testNotAccessible() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE }

        composeTestRule.setContent {
            StopHeader(RouteCardData.RouteStopData(stop, emptyList(), emptyList()), true)
        }
        composeTestRule.onNodeWithText(stop.name).assertIsDisplayed()
        composeTestRule.onNodeWithText("Not accessible").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wheelchair_accessible").assertDoesNotExist()
    }

    @Test
    fun testElevatorAlert() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE }
        val alert = objects.alert { effect = Alert.Effect.ElevatorClosure }

        composeTestRule.setContent {
            StopHeader(
                RouteCardData.RouteStopData(
                    stop,
                    emptyList(),
                    listOf(
                        RouteCardData.Leaf(
                            0,
                            emptyList(),
                            emptySet(),
                            emptyList(),
                            listOf(alert),
                            true,
                            true
                        )
                    )
                ),
                true
            )
        }
        composeTestRule.onNodeWithText(stop.name).assertIsDisplayed()
        composeTestRule.onNodeWithText("1 elevator closed").assertIsDisplayed()
        composeTestRule.onNodeWithTag("elevator_alert").assertIsDisplayed()
    }
}
