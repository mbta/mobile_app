package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.testUtils.assertCanBeDisplayed
import com.mbta.tid.mbta_app.android.testUtils.hasClickActionLabel
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TrackChange
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.stopDetailsPage.TileData
import kotlin.test.assertTrue
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test

class DepartureTileTest {
    @get:Rule val composeTestRule = createComposeRule(effectContext = UnconfinedTestDispatcher())

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
                        warningAlert = null,
                    ),
                    upcoming,
                ),
                onTap = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("headsign").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("5 min").assertCanBeDisplayed()
    }

    @Test
    fun testRoutePill() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route {
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
                        warningAlert = null,
                    ),
                    upcoming,
                ),
                onTap = {},
                showRoutePill = true,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("B").assertCanBeDisplayed()
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
                        warningAlert = null,
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
                        warningAlert = null,
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

    @Test
    fun testTrackChangeBadgeShownWithTrackChangeAlert() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.COMMUTER_RAIL }
        val trip = objects.trip()
        val alert = objects.alert { effect = Alert.Effect.TrackChange }
        val upcoming = UpcomingTrip(trip, trackChange = TrackChange.Alert(alert))
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
                        warningAlert = null,
                    ),
                    upcoming,
                ),
                onTap = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag("track_change_pill", useUnmergedTree = true)
            .assertCanBeDisplayed()
        // The tile only has room for the compact badge, not the expanded pill with a label
        composeTestRule.onNodeWithText("Track change", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("5 min").assertCanBeDisplayed()
    }

    @Test
    fun testTrackChangeBadgeShownAlongsideRoutePill() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route {
            id = "Green-B"
            longName = "Green Line B"
            shortName = "B"
            type = RouteType.LIGHT_RAIL
        }
        val trip = objects.trip()
        val alert = objects.alert { effect = Alert.Effect.TrackChange }
        val upcoming = UpcomingTrip(trip, trackChange = TrackChange.Alert(alert))
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
                        warningAlert = null,
                    ),
                    upcoming,
                ),
                onTap = {},
                showRoutePill = true,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag("track_change_pill", useUnmergedTree = true)
            .assertCanBeDisplayed()
        composeTestRule.onNodeWithText("B", useUnmergedTree = true).assertCanBeDisplayed()
    }

    @Test
    fun testNoTrackChangeBadgeWithoutTrackChangeAlert() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.COMMUTER_RAIL }
        val trip = objects.trip()
        // A non-track-change alert should not result in a badge
        val otherAlert = objects.alert { effect = Alert.Effect.Delay }
        val upcoming = UpcomingTrip(trip, alert = otherAlert)
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
                        warningAlert = null,
                    ),
                    upcoming,
                ),
                onTap = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("5 min").assertCanBeDisplayed()
        composeTestRule
            .onNodeWithTag("track_change_pill", useUnmergedTree = true)
            .assertDoesNotExist()
    }
}
