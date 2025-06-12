package com.mbta.tid.mbta_app.android.routeDetails

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteDetailsStopList
import com.mbta.tid.mbta_app.model.RouteType
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class CollapsableStopListTest {
    @get:Rule val composeTestRule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testWhenOneStopJustShowsThatStop() {
        val objects = ObjectCollectionBuilder()
        val stop1 =
            objects.stop {
                name = "Stop 1"
                locationType = LocationType.STATION
            }
        var clicked = false
        val mainRoute =
            objects.route {
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Here", "There")
                longName = "Mauve Line"
                type = RouteType.HEAVY_RAIL
            }
        composeTestRule.setContent {
            CollapsableStopList(
                RouteCardData.LineOrRoute.Route(mainRoute),
                segment =
                    RouteDetailsStopList.Segment(
                        listOf(RouteDetailsStopList.Entry(stop1, listOf(), listOf())),
                        hasRouteLine = false,
                    ),
                onClick = { clicked = true },
                isFirstSegment = false,
                isLastSegment = false,
                { _, _ -> },
            )
        }

        composeTestRule.waitUntilExactlyOneExists(hasText(stop1.name))
        composeTestRule.onNodeWithText("Less common stop").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mbta_logo").assertIsDisplayed()
        composeTestRule.onNodeWithText(stop1.name).performClick()
        assertTrue(clicked)
    }

    @Test
    fun testWhenMultipleStopsCanExpandAndCollapse() {
        val objects = ObjectCollectionBuilder()
        val stop1 =
            objects.stop {
                name = "Stop 1"
                locationType = LocationType.STATION
            }
        val stop2 =
            objects.stop {
                name = "Stop 2"
                locationType = LocationType.STOP
                vehicleType = RouteType.BUS
            }
        val mainRoute =
            objects.route {
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Here", "There")
                longName = "Mauve Line"
                type = RouteType.BUS
            }
        composeTestRule.setContent {
            CollapsableStopList(
                RouteCardData.LineOrRoute.Route(mainRoute),
                segment =
                    RouteDetailsStopList.Segment(
                        listOf(
                            RouteDetailsStopList.Entry(stop1, listOf(), listOf()),
                            RouteDetailsStopList.Entry(stop2, listOf(), listOf()),
                        ),
                        hasRouteLine = false,
                    ),
                onClick = {},
                isFirstSegment = false,
                isLastSegment = false,
                { _, _ -> },
            )
        }
        composeTestRule.onNodeWithText(stop1.name).assertIsNotDisplayed()

        composeTestRule.onNodeWithText("2 less common stops").assertIsDisplayed().performClick()
        composeTestRule.waitUntilExactlyOneExists(hasText(stop1.name))
        composeTestRule.onNodeWithTag("mbta_logo").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stop_bus").assertIsDisplayed()
        composeTestRule.onNodeWithText(stop2.name).assertIsDisplayed()

        composeTestRule.onNodeWithText("2 less common stops").assertIsDisplayed().performClick()
        composeTestRule.waitUntilDoesNotExist(hasText(stop1.name))
    }
}
