package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.hasClickActionLabel
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.stopDetailsPage.TileData
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class DepartureTileTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testBasic() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val trip = objects.trip()
        val upcoming = UpcomingTrip(trip)
        composeTestRule.setContent {
            DepartureTile(
                TileData(
                    route,
                    "headsign",
                    UpcomingFormat.Some(
                        trips =
                            listOf(
                                UpcomingFormat.Some.FormattedTrip(
                                    upcoming,
                                    route.type,
                                    TripInstantDisplay.Minutes(5, false),
                                    false,
                                )
                            ),
                        secondaryAlert = null,
                    ),
                    upcoming,
                ),
                onTap = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("headsign").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 min").assertIsDisplayed()
    }

    @Test
    fun testRoutePill() {
        val objects = ObjectCollectionBuilder()
        val route =
            objects.route {
                id = "Green-B"
                longName = "Green Line B"
                shortName = "B"
                type = RouteType.LIGHT_RAIL
            }
        val trip = objects.trip()
        val upcoming = UpcomingTrip(trip)
        composeTestRule.setContent {
            DepartureTile(
                TileData(
                    route,
                    "headsign",
                    UpcomingFormat.Some(
                        trips =
                            listOf(
                                UpcomingFormat.Some.FormattedTrip(
                                    upcoming,
                                    route.type,
                                    TripInstantDisplay.Minutes(5, false),
                                    false,
                                )
                            ),
                        secondaryAlert = null,
                    ),
                    upcoming,
                ),
                onTap = {},
                showRoutePill = true,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("B").assertIsDisplayed()
    }

    @Test
    fun testTap() {
        var tapped = false
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val trip = objects.trip()
        val upcoming = UpcomingTrip(trip)
        composeTestRule.setContent {
            DepartureTile(
                TileData(
                    route,
                    "headsign",
                    UpcomingFormat.Some(
                        trips =
                            listOf(
                                UpcomingFormat.Some.FormattedTrip(
                                    upcoming,
                                    route.type,
                                    TripInstantDisplay.Minutes(5, false),
                                    false,
                                )
                            ),
                        secondaryAlert = null,
                    ),
                    upcoming,
                ),
                onTap = { tapped = true },
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("headsign").performClick()
        composeTestRule.waitForIdle()
        assertTrue(tapped)
    }

    @Test
    fun testAccessibility() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val trip = objects.trip()
        val upcoming = UpcomingTrip(trip)

        var selected by mutableStateOf(false)
        composeTestRule.setContent {
            DepartureTile(
                TileData(
                    route,
                    "headsign",
                    UpcomingFormat.Some(
                        trips =
                            listOf(
                                UpcomingFormat.Some.FormattedTrip(
                                    upcoming,
                                    route.type,
                                    TripInstantDisplay.Minutes(5, false),
                                    false,
                                )
                            ),
                        secondaryAlert = null,
                    ),
                    upcoming,
                ),
                onTap = {},
                isSelected = selected,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNode(hasClickAction())
            .assert(hasClickActionLabel("display more information about this trip"))

        selected = true
        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasClickAction()).assert(hasClickActionLabel(null))
    }
}
