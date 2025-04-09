package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus
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
            directionNames = listOf("North", "South")
            directionDestinations = listOf("Downtown", "Uptown")
            longName = "Sample Route Long Name"
            shortName = "Sample Route"
            lineId = "line_1"
            routePatternIds = mutableListOf("pattern_1", "pattern_2")
        }
    val line =
        builder.line {
            id = "line_1"
            longName = "Sample Line Long Name"
            shortName = "Sample Line"
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
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
        }
    val inaccessibleStop =
        builder.stop {
            id = "stop_2"
            name = "Sample Stop"
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE
        }
    val trip =
        builder.trip(routePattern) {
            id = "trip_1"
            headsign = "Sample Headsign"
        }
    val elevatorAlert =
        builder.alert {
            effect = Alert.Effect.ElevatorClosure
            header = "Test elevator alert"
        }

    private fun makePatterns(stop: Stop, elevatorAlerts: List<Alert> = emptyList()) =
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
            ),
            elevatorAlerts
        )

    val patternsByStop = makePatterns(stop)
    val elevatorAlertPatternsByStop = makePatterns(stop, listOf(elevatorAlert))
    val inaccessiblePatternsByStop = makePatterns(inaccessibleStop)

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testNearbyStopViewDisplaysCorrectly() {
        composeTestRule.setContent {
            NearbyStopView(
                patternsAtStop = patternsByStop,
                condenseHeadsignPredictions = false,
                now = now,
                pinned = false,
                onOpenStopDetails = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Sample Stop").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sample Headsign").assertIsDisplayed()
        composeTestRule.onNodeWithText("Now").assertIsDisplayed()
    }

    @Test
    fun testNearbyStopViewDisplaysElevatorAlerts() {
        composeTestRule.setContent {
            NearbyStopView(
                patternsAtStop = elevatorAlertPatternsByStop,
                condenseHeadsignPredictions = false,
                now = now,
                pinned = false,
                onOpenStopDetails = { _, _ -> },
                showStationAccessibility = true
            )
        }
        composeTestRule.onNodeWithText("1 elevator closed").assertIsDisplayed()
        composeTestRule.onNodeWithTag("elevator_alert").assertIsDisplayed()
    }

    @Test
    fun testNearbyStopViewDisplaysWheelchairAccessibilityAlerts() {
        composeTestRule.setContent {
            NearbyStopView(
                patternsAtStop = inaccessiblePatternsByStop,
                condenseHeadsignPredictions = false,
                now = now,
                pinned = false,
                onOpenStopDetails = { _, _ -> },
                showStationAccessibility = true
            )
        }
        composeTestRule.onNodeWithText("Not accessible").assertIsDisplayed()
        composeTestRule.onNodeWithTag("elevator_alert").assertDoesNotExist()
        composeTestRule.onNodeWithTag("wheelchair_accessible").assertDoesNotExist()
    }

    @Test
    fun testNearbyStopViewDisplaysWheelchairAccessibility() {
        composeTestRule.setContent {
            NearbyStopView(
                patternsAtStop = patternsByStop,
                condenseHeadsignPredictions = false,
                now = now,
                pinned = false,
                onOpenStopDetails = { _, _ -> },
                showStationAccessibility = true
            )
        }
        composeTestRule.onNodeWithText("Not accessible").assertDoesNotExist()
        composeTestRule.onNodeWithTag("elevator_alert").assertDoesNotExist()
        composeTestRule.onNodeWithTag("wheelchair_accessible").assertIsDisplayed()
    }
}
