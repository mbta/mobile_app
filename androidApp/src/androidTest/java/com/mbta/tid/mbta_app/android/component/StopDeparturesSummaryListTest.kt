package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingTrip
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test

class StopDeparturesSummaryListTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testHandlesByHeadsign() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val routePattern =
            objects.routePattern(route) { representativeTrip { headsign = "Alewife" } }
        val stop = objects.stop()
        val trip = objects.trip(routePattern)
        val prediction =
            objects.prediction {
                this.trip = trip
                this.departureTime = now + 5.499.minutes
            }
        val patterns =
            listOf(
                RealtimePatterns.ByHeadsign(
                    route,
                    "Alewife",
                    null,
                    listOf(routePattern),
                    listOf(UpcomingTrip(trip, prediction))
                )
            )

        composeTestRule.setContent {
            StopDeparturesSummaryList(
                patternsAtStop = PatternsByStop(route, stop, patterns),
                condenseHeadsignPredictions = false,
                now = now,
                context = TripInstantDisplay.Context.NearbyTransit
            )
        }

        composeTestRule.onNodeWithText("Alewife").assertExists()
        composeTestRule.onNodeWithText("5").assertExists()
        composeTestRule.onNodeWithText("min").assertExists()
    }
}
