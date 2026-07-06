package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.android.testUtils.assertCanBeDisplayed
import com.mbta.tid.mbta_app.android.testUtils.hasTextMatching
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSummaryEntity
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Month
import org.junit.Rule
import org.junit.Test

class TripStopRowTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testStopName() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "Worcester" }
        val trip = objects.trip()
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
                    vehicle = null,
                    routes = listOf(route),
                ),
                trip,
                now,
                onTapLink = {},
                onOpenAlertDetails = {},
                route,
                TripRouteAccents(route),
            )
        }

        composeTestRule.onNodeWithText(stop.name).assertCanBeDisplayed()
    }

    @Test
    fun testPrediction() {
        val now = EasternTimeInstant(2025, Month.JANUARY, 24, 15, 37, 39)
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val trip = objects.trip()
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
                    vehicle = null,
                    routes = listOf(route),
                ),
                trip,
                now,
                onTapLink = {},
                onOpenAlertDetails = {},
                route,
                TripRouteAccents(route),
            )
        }

        composeTestRule
            .onNode(hasTextMatching(Regex("3:37\\sPM", RegexOption.IGNORE_CASE)))
            .assertCanBeDisplayed()
    }

    @Test
    fun testTrackNumber() {
        val now = EasternTimeInstant(2025, Month.JANUARY, 24, 15, 37, 39)
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { id = "place-bbsta" }
        val platformStop = objects.stop {
            platformCode = "2"
            vehicleType = RouteType.COMMUTER_RAIL
            parentStationId = stop.id
        }
        val trip = objects.trip()
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
                    predictionStop = platformStop,
                    vehicle = null,
                    routes = listOf(route),
                ),
                trip,
                now,
                onTapLink = {},
                onOpenAlertDetails = {},
                route,
                TripRouteAccents(route),
            )
        }

        composeTestRule.onNodeWithText("Track 2").assertCanBeDisplayed()
        composeTestRule.onNodeWithContentDescription("Boarding on track 2").assertCanBeDisplayed()
    }

    @Test
    fun testAccessibility() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "stop" }
        val trip = objects.trip()
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
                vehicle = null,
                routes = listOf(route),
            )

        var selected by mutableStateOf(false)
        var first by mutableStateOf(false)

        composeTestRule.setContent {
            TripStopRow(
                stopEntry,
                trip,
                now,
                onTapLink = {},
                onOpenAlertDetails = {},
                route,
                TripRouteAccents(route),
                targeted = selected,
                firstStop = first,
            )
        }

        composeTestRule.onNodeWithContentDescription("stop").assertCanBeDisplayed()

        selected = true

        composeTestRule.onNodeWithContentDescription("stop, selected stop").assertCanBeDisplayed()

        first = true

        composeTestRule
            .onNodeWithContentDescription("stop, selected stop, first stop")
            .assertCanBeDisplayed()

        selected = false
        composeTestRule.onNodeWithContentDescription("stop, first stop").assertCanBeDisplayed()
    }

    @Test
    fun testClickable() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "Worcester" }
        val trip = objects.trip()
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
                vehicle = null,
                routes = listOf(route),
            )
        var linkTappedWith: TripDetailsStopList.Entry? = null

        composeTestRule.setContent {
            TripStopRow(
                entry,
                trip,
                now,
                onTapLink = { linkTappedWith = it },
                onOpenAlertDetails = {},
                route,
                TripRouteAccents(route),
            )
        }

        composeTestRule.onNodeWithText(stop.name).performClick()
        assertEquals(entry, linkTappedWith)
    }

    @Test
    fun testStationAccessibility() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val accessibleStop = objects.stop {
            name = "Park Street"
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
        }
        val inaccessibleStop = objects.stop {
            name = "Boylston"
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE
        }
        val trip = objects.trip()
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
                vehicle = null,
                routes = listOf(route),
                elevatorAlerts = elevatorAlerts,
            )

        loadKoinMocks {
            settings = MockSettingsRepository(mapOf(Settings.StationAccessibility to true))
        }

        var testEntry by mutableStateOf(entry(inaccessibleStop))
        composeTestRule.setContent {
            TripStopRow(
                testEntry,
                trip,
                now,
                onTapLink = {},
                onOpenAlertDetails = {},
                route,
                TripRouteAccents(route),
            )
        }

        composeTestRule
            .onNodeWithTag("wheelchair_not_accessible", useUnmergedTree = true)
            .assertCanBeDisplayed()
        composeTestRule
            .onNodeWithContentDescription("This stop is not accessible", useUnmergedTree = true)
            .assertCanBeDisplayed()

        composeTestRule.onNodeWithTag("elevator_alert", useUnmergedTree = true).assertDoesNotExist()

        testEntry = entry(accessibleStop)
        composeTestRule
            .onNodeWithTag("wheelchair_not_accessible", useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag("elevator_alert", useUnmergedTree = true).assertDoesNotExist()

        testEntry =
            entry(
                accessibleStop,
                listOf(objects.alert {}),
            )
        composeTestRule
            .onNodeWithTag("wheelchair_not_accessible", useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithTag("elevator_alert", useUnmergedTree = true)
            .assertCanBeDisplayed()
        composeTestRule
            .onNodeWithContentDescription("This stop has 1 elevator closed", useUnmergedTree = true)
            .assertCanBeDisplayed()
    }

    @Test
    fun testAlertCard() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route()
        val alert = objects.alert {
            effect = Alert.Effect.Shuttle
            summaries =
                listOf(
                    AlertSummaryEntity(
                        null,
                        null,
                        null,
                        null,
                        "**Shuttle buses** from **Roxbury Crossing** to **Green Street** through tomorrow",
                    )
                )
        }

        val trip = objects.trip()
        val entry =
            TripDetailsStopList.Entry(
                stop,
                0,
                UpcomingFormat.Disruption(alert, MapStopRoute.ORANGE),
                schedule = null,
                prediction = null,
                vehicle = null,
                routes = emptyList(),
            )

        composeTestRule.setContent {
            TripStopRow(
                entry,
                trip,
                now,
                {},
                {},
                route,
                TripRouteAccents(route),
                showDownstreamAlert = true,
            )
        }

        composeTestRule
            .onNodeWithText("Shuttle buses from Roxbury Crossing to Green Street through tomorrow")
            .assertCanBeDisplayed()
    }
}
