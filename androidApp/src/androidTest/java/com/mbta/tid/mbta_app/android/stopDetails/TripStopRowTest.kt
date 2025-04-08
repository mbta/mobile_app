package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.hasTextMatching
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Rule
import org.junit.Test

class TripStopRowTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testStopName() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "Worcester" }
        val schedule = objects.schedule { departureTime = now + 5.seconds }
        val prediction = objects.prediction(schedule) { departureTime = now + 6.seconds }
        val route = objects.route()

        composeTestRule.setContent {
            TripStopRow(
                TripDetailsStopList.Entry(
                    stop,
                    0,
                    null,
                    schedule,
                    prediction,
                    stop,
                    null,
                    listOf(route)
                ),
                now,
                onTapLink = {},
                TripRouteAccents(route)
            )
        }

        composeTestRule.onNodeWithText(stop.name).assertIsDisplayed()
    }

    @Test
    fun testPrediction() {
        val now =
            LocalDateTime.parse("2025-01-24T15:37:39").toInstant(TimeZone.currentSystemDefault())
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val schedule = objects.schedule { departureTime = now + 5.seconds }
        val prediction = objects.prediction(schedule) { departureTime = now + 6.seconds }
        val route = objects.route()

        composeTestRule.setContent {
            TripStopRow(
                TripDetailsStopList.Entry(
                    stop,
                    0,
                    null,
                    schedule,
                    prediction,
                    stop,
                    null,
                    listOf(route)
                ),
                now,
                onTapLink = {},
                TripRouteAccents(route)
            )
        }

        composeTestRule.onNode(hasTextMatching(Regex("3:37\\sPM"))).assertIsDisplayed()
    }

    @Test
    fun testTrackNumber() {
        val now =
            LocalDateTime.parse("2025-01-24T15:37:39").toInstant(TimeZone.currentSystemDefault())
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { id = "place-bbsta" }
        val platformStop =
            objects.stop {
                platformCode = "2"
                vehicleType = RouteType.COMMUTER_RAIL
                parentStationId = stop.id
            }
        val schedule = objects.schedule { departureTime = now + 5.seconds }
        val prediction = objects.prediction(schedule) { departureTime = now + 6.seconds }
        val route = objects.route { type = RouteType.COMMUTER_RAIL }

        composeTestRule.setContent {
            TripStopRow(
                TripDetailsStopList.Entry(
                    stop,
                    0,
                    null,
                    schedule,
                    prediction,
                    platformStop,
                    null,
                    listOf(route)
                ),
                now,
                onTapLink = {},
                TripRouteAccents(route)
            )
        }

        composeTestRule.onNodeWithText("Track 2").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Boarding on track 2").assertIsDisplayed()
    }

    @Test
    fun testAccessibility() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "stop" }
        val schedule = objects.schedule { departureTime = now + 5.seconds }
        val prediction = objects.prediction(schedule) { departureTime = now + 6.seconds }
        val route = objects.route()

        val stopEntry =
            TripDetailsStopList.Entry(
                stop,
                0,
                null,
                schedule,
                prediction,
                stop,
                null,
                listOf(route)
            )

        var selected by mutableStateOf(false)
        var first by mutableStateOf(false)

        composeTestRule.setContent {
            TripStopRow(
                stopEntry,
                now,
                onTapLink = {},
                TripRouteAccents(route),
                targeted = selected,
                firstStop = first
            )
        }

        composeTestRule.onNodeWithContentDescription("stop").assertIsDisplayed()

        selected = true

        composeTestRule.onNodeWithContentDescription("stop, selected stop").assertIsDisplayed()

        first = true

        composeTestRule
            .onNodeWithContentDescription("stop, selected stop, first stop")
            .assertIsDisplayed()

        selected = false
        composeTestRule.onNodeWithContentDescription("stop, first stop").assertIsDisplayed()
    }

    @Test
    fun testClickable() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "Worcester" }
        val schedule = objects.schedule { departureTime = now + 5.seconds }
        val prediction = objects.prediction(schedule) { departureTime = now + 6.seconds }
        val route = objects.route()

        val entry =
            TripDetailsStopList.Entry(
                stop,
                0,
                null,
                schedule,
                prediction,
                stop,
                null,
                listOf(route)
            )
        var linkTappedWith: TripDetailsStopList.Entry? = null

        composeTestRule.setContent {
            TripStopRow(entry, now, onTapLink = { linkTappedWith = it }, TripRouteAccents(route))
        }

        composeTestRule.onNodeWithText(stop.name).performClick()
        assertEquals(entry, linkTappedWith)
    }

    @Test
    fun testElevatorAccessibility() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val accessibleStop =
            objects.stop {
                name = "Park Street"
                wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
            }
        val inaccessibleStop =
            objects.stop {
                name = "Boylston"
                wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE
            }
        val schedule = objects.schedule { departureTime = now + 5.seconds }
        val prediction = objects.prediction(schedule) { departureTime = now + 6.seconds }
        val route = objects.route()

        fun entry(stop: Stop, elevatorAlerts: List<Alert> = emptyList()) =
            TripDetailsStopList.Entry(
                stop,
                0,
                null,
                schedule,
                prediction,
                stop,
                null,
                listOf(route),
                elevatorAlerts
            )

        var testEntry by mutableStateOf(entry(inaccessibleStop))
        composeTestRule.setContent {
            TripStopRow(
                testEntry,
                now,
                onTapLink = {},
                TripRouteAccents(route),
                showElevatorAccessibility = true
            )
        }

        composeTestRule.onNodeWithTag("wheelchair_not_accessible").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Not accessible").assertIsDisplayed()

        composeTestRule.onNodeWithTag("elevator_alert").assertDoesNotExist()

        testEntry = entry(accessibleStop)
        composeTestRule.onNodeWithTag("wheelchair_not_accessible").assertDoesNotExist()
        composeTestRule.onNodeWithTag("elevator_alert").assertDoesNotExist()

        testEntry =
            entry(
                accessibleStop,
                listOf(objects.alert { activePeriod(now.minus(20.minutes), now.plus(20.minutes)) })
            )
        composeTestRule.onNodeWithTag("wheelchair_not_accessible").assertDoesNotExist()
        composeTestRule.onNodeWithTag("elevator_alert").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("1 elevator closed").assertIsDisplayed()
    }
}
