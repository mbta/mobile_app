package com.mbta.tid.mbta_app.android.alertDetails

import android.icu.text.DateFormat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.mbta.tid.mbta_app.android.hasTextMatching
import com.mbta.tid.mbta_app.android.util.toJavaDate
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import java.util.Calendar
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinInstant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Rule
import org.junit.Test

class AlertDetailsTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testBasicAlertDetails() {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop { name = "Stop 1" }
        val stop2 = objects.stop { name = "Stop 2" }
        val stop3 = objects.stop { name = "Stop 3" }

        val route =
            objects.route {
                type = RouteType.HEAVY_RAIL
                longName = "Red Line"
            }

        val now = Clock.System.now()

        val alert =
            objects.alert {
                activePeriod(now - 5.minutes, now + 5.minutes)
                description = "Long description"
                cause = Alert.Cause.UnrulyPassenger
                effect = Alert.Effect.StopClosure
                effectName = "Closure"
                header = "Alert header"
                updatedAt =
                    LocalDateTime.parse("2025-01-22T10:36:13")
                        .toInstant(TimeZone.currentSystemDefault())
            }

        composeTestRule.setContent {
            AlertDetails(
                alert,
                null,
                listOf(route),
                stop = null,
                affectedStops = listOf(stop1, stop2, stop3),
                now = now,
            )
        }

        composeTestRule.onNodeWithText("Red Line Stop Closure").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unruly Passenger").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                alert.activePeriod[0].formatStart(ApplicationProvider.getApplicationContext()).text
            )
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("3 affected stops").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 affected stops").performClick()
        composeTestRule.onNodeWithText(stop1.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop2.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop3.name).assertIsDisplayed()
        composeTestRule.onNodeWithText("3 affected stops").performClick()
        composeTestRule.onNodeWithText("Full Description").assertIsDisplayed()
        composeTestRule.onNodeWithText(alert.description!!).assertIsDisplayed()
        composeTestRule.onNodeWithText(alert.header!!).assertIsDisplayed()
        // as of probably API 34, this has a U+202F Narrow No-Break Space instead of U+0020 Space,
        // so to make the test work either way we need a regex
        composeTestRule
            .onNode(hasTextMatching(Regex("Updated: 1/22/25, 10:36\\sAM")))
            .assertIsDisplayed()
    }

    @Test
    fun testCurrentActivePeriod() {
        val objects = ObjectCollectionBuilder()

        val now = Clock.System.now()

        val alert =
            objects.alert {
                activePeriod(now - 20.minutes, now - 10.minutes)
                activePeriod(now - 5.minutes, now + 5.minutes)
                activePeriod(now + 10.minutes, now + 20.minutes)
                updatedAt = now - 100.minutes
            }

        composeTestRule.setContent {
            AlertDetails(
                alert,
                line = null,
                routes = null,
                stop = null,
                affectedStops = emptyList(),
                now = now,
            )
        }

        composeTestRule
            .onNodeWithText(
                alert.activePeriod[1].formatStart(ApplicationProvider.getApplicationContext()).text
            )
            .assertIsDisplayed()
    }

    @Test
    fun testServiceStartAndEndActivePeriod() {
        val objects = ObjectCollectionBuilder()

        val now = Clock.System.now()

        val calendar = Calendar.getInstance()
        calendar.time = now.toJavaDate()
        calendar.timeZone = java.util.TimeZone.getTimeZone("America/New_York")

        val startCalendar = calendar.clone() as Calendar
        startCalendar.set(Calendar.HOUR_OF_DAY, 3)
        startCalendar.set(Calendar.MINUTE, 0)

        val endCalendar = calendar.clone() as Calendar
        endCalendar.set(Calendar.HOUR_OF_DAY, 2)
        endCalendar.set(Calendar.MINUTE, 59)
        endCalendar.add(Calendar.DAY_OF_YEAR, 1)

        val start = startCalendar.toInstant().toKotlinInstant()
        val end = endCalendar.toInstant().toKotlinInstant()
        val alert =
            objects.alert {
                activePeriod(start, end)
                updatedAt = now - 100.minutes
            }

        composeTestRule.setContent {
            AlertDetails(
                alert,
                line = null,
                routes = null,
                stop = null,
                affectedStops = emptyList(),
                now = now,
            )
        }

        val time = DateFormat.getInstanceForSkeleton(DateFormat.HOUR_MINUTE)
        composeTestRule
            .onNode(
                hasText("start of service", substring = true) and
                    hasText(" ${calendar.get(Calendar.DAY_OF_MONTH)},", substring = true)
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(time.format(start.toJavaDate()), substring = true)
            .assertDoesNotExist()
        composeTestRule
            .onNode(
                hasText("end of service", substring = true) and
                    hasText(" ${calendar.get(Calendar.DAY_OF_MONTH)},", substring = true)
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(time.format(end.toJavaDate()), substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun testLaterTodayActivePeriod() {
        val objects = ObjectCollectionBuilder()

        val now = Clock.System.now()
        val start = now - 20.minutes
        val end = now + 2.hours

        val alert =
            objects.alert {
                durationCertainty = Alert.DurationCertainty.Estimated
                activePeriod(start, end)
                updatedAt = now - 100.minutes
            }

        composeTestRule.setContent {
            AlertDetails(
                alert,
                line = null,
                routes = null,
                stop = null,
                affectedStops = emptyList(),
                now = now,
            )
        }

        val time = DateFormat.getInstanceForSkeleton(DateFormat.HOUR_MINUTE)
        composeTestRule
            .onNodeWithText(time.format(start.toJavaDate()), substring = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(time.format(end.toJavaDate()), substring = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("later today", substring = true).assertIsDisplayed()
    }

    @Test
    fun testNoCurrentActivePeriod() {
        val objects = ObjectCollectionBuilder()

        val now = Clock.System.now()

        val alert =
            objects.alert {
                activePeriod(now - 10.minutes, now - 5.minutes)
                activePeriod(now + 5.minutes, now + 10.minutes)
                cause = Alert.Cause.UnrulyPassenger
                effect = Alert.Effect.StopClosure
                effectName = "Closure"
                updatedAt = now - 100.minutes
            }

        composeTestRule.setContent {
            AlertDetails(
                alert,
                line = null,
                routes = null,
                stop = null,
                affectedStops = emptyList(),
                now = now,
            )
        }

        composeTestRule.onNodeWithText("Alert is no longer in effect").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start").assertDoesNotExist()
    }

    @Test
    fun testNoDescription() {
        val objects = ObjectCollectionBuilder()

        val now = Clock.System.now()

        val alert =
            objects.alert {
                activePeriod(now - 5.minutes, now + 5.minutes)
                cause = Alert.Cause.UnrulyPassenger
                effect = Alert.Effect.StopClosure
                effectName = "Closure"
                updatedAt = now - 100.minutes
            }

        composeTestRule.setContent {
            AlertDetails(
                alert,
                line = null,
                routes = null,
                stop = null,
                affectedStops = emptyList(),
                now = now,
            )
        }

        composeTestRule.onNodeWithText("Full Description").assertDoesNotExist()
    }

    @Test
    fun testStopsInDescription() {
        val objects = ObjectCollectionBuilder()

        val now = Clock.System.now()

        val stop1 = objects.stop { name = "Stop 1" }
        val stop2 = objects.stop { name = "Stop 2" }
        val stop3 = objects.stop { name = "Stop 3" }

        val alert =
            objects.alert {
                activePeriod(now - 5.minutes, now + 5.minutes)
                cause = Alert.Cause.UnrulyPassenger
                effect = Alert.Effect.StopClosure
                effectName = "Closure"
                updatedAt = now - 100.minutes
                header = "Alert header"
                description =
                    "Alert description\n\nAffected stops:\nStop 1\nStop 2\nStop 3\n\nMore details"
            }

        val affectedStops = mutableStateOf(emptyList<Stop>())

        composeTestRule.setContent {
            AlertDetails(
                alert,
                line = null,
                routes = null,
                stop = null,
                affectedStops = affectedStops.value,
                now = now,
            )
        }

        composeTestRule.onNodeWithText("3 affected stops").assertDoesNotExist()
        composeTestRule.onNodeWithText("Alert description").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Affected stops:\nStop 1\nStop 2\nStop 3")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("More details").assertIsDisplayed()

        affectedStops.value = listOf(stop1, stop2, stop3)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("3 affected stops").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alert description").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Affected stops:\nStop 1\nStop 2\nStop 3")
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("More details").assertIsDisplayed()
    }

    @Test
    fun testStopHeader() {
        val objects = ObjectCollectionBuilder()

        val now = Clock.System.now()

        val stop = objects.stop { name = "Stop" }

        val alert =
            objects.alert {
                activePeriod(now - 5.minutes, now + 5.minutes)
                cause = Alert.Cause.UnrulyPassenger
                effect = Alert.Effect.StopClosure
                effectName = "Closure"
                updatedAt = now - 100.minutes
                header = "Alert header"
                description = "Alert description"
            }

        composeTestRule.setContent {
            AlertDetails(
                alert,
                line = null,
                routes = null,
                stop = stop,
                affectedStops = listOf(stop),
                now = now,
            )
        }

        composeTestRule.onNodeWithText("${stop.name} Stop Closure").assertExists()
    }

    @Test
    fun testElevatorAlert() {
        val objects = ObjectCollectionBuilder()

        val now = Clock.System.now()

        val stop = objects.stop { name = "Stop" }

        val alert =
            objects.alert {
                activePeriod(now - 5.minutes, now + 5.minutes)
                cause = Alert.Cause.Maintenance
                effect = Alert.Effect.ElevatorClosure
                effectName = "Elevator Closure"
                updatedAt = now - 100.minutes
                header = "Alert header"
                description = "Alert description"
            }

        composeTestRule.setContent {
            AlertDetails(
                alert,
                line = null,
                routes = null,
                stop = stop,
                affectedStops = listOf(stop),
                now = now,
            )
        }

        composeTestRule.onNodeWithText("${stop.name} Elevator Closure").assertIsDisplayed()
        composeTestRule.onNodeWithText(alert.header!!).assertIsDisplayed()
        composeTestRule.onNodeWithText("Alternative path").assertIsDisplayed()
        composeTestRule.onNodeWithText(alert.description!!).assertIsDisplayed()
        composeTestRule.onNodeWithText("1 affected stop").assertDoesNotExist()
    }
}
