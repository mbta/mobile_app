package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.UpcomingTrip
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test

class NearbyStopViewTest {
    val builder = ObjectCollectionBuilder()
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    val route =
        builder.route {
            id = "route_1"
            type = RouteType.BUS
            color = "FF0000"
            directionNames = listOf("North", "South")
            directionDestinations = listOf("Downtown", "Uptown")
            longName = "Sample Route Long Name"
            shortName = "Sample Route"
            textColor = "000000"
            lineId = "line_1"
            routePatternIds = mutableListOf("pattern_1", "pattern_2")
        }
    val line =
        builder.line {
            id = "line_1"
            color = "FF0000"
            longName = "Sample Line Long Name"
            shortName = "Sample Line"
            textColor = "000000"
        }
    val routePattern =
        builder.routePattern(route) {
            id = "pattern_1"
            directionId = 0
            name = "Sample Route Pattern"
            routeId = "route_1"
            representativeTripId = "trip_1"
        }
    val stop =
        builder.stop {
            id = "stop_1"
            name = "Sample Stop"
        }
    val trip =
        builder.trip(routePattern) {
            id = "trip_1"
            headsign = "Sample Headsign"
        }

    val patternsByStop =
        PatternsByStop(
            route,
            stop,
            listOf(
                RealtimePatterns.ByHeadsign(
                    route,
                    "Sample Headsign",
                    line,
                    listOf(routePattern),
                    listOf(
                        UpcomingTrip(
                            trip,
                            Prediction(
                                "prediction_1",
                                now,
                                now.plus(1.minutes),
                                0,
                                false,
                                Prediction.ScheduleRelationship.Scheduled,
                                null,
                                1,
                                route.id,
                                stop.id,
                                trip.id,
                                null
                            )
                        )
                    )
                )
            )
        )

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testNearbyStopViewDisplaysCorrectly() {
        composeTestRule.setContent {
            NearbyStopView(
                patternsAtStop = patternsByStop,
                condenseHeadsignPredictions = false,
                now = now,
                onOpenStopDetails = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Sample Stop").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sample Headsign").assertIsDisplayed()
        composeTestRule.onNodeWithText("ARR").assertIsDisplayed()
    }
}
