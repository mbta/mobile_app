package com.mbta.tid.mbta_app.android.alertDetails

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.hasTextMatching
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Month
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

        val now = EasternTimeInstant(2025, Month.JANUARY, 22, 10, 40)

        val alert =
            objects.alert {
                activePeriod(now - 5.minutes, now + 5.minutes)
                description = "Long description"
                cause = Alert.Cause.UnrulyPassenger
                effect = Alert.Effect.StopClosure
                effectName = "Closure"
                header = "Alert header"
                updatedAt = EasternTimeInstant(2025, Month.JANUARY, 22, 10, 36, 13)
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
            .onNode(hasTextMatching(Regex("Wednesday, Jan 22, 10:35\\sAM")))
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

        val now = EasternTimeInstant(2025, Month.JULY, 29, 9, 30)

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
            .onNode(hasTextMatching(Regex("Tuesday, Jul 29, 9:25\\sAM")))
            .assertIsDisplayed()
    }

    @Test
    fun testServiceStartAndEndActivePeriod() {
        val objects = ObjectCollectionBuilder()

        val now = EasternTimeInstant(2025, Month.JULY, 29, 9, 10)

        val start = EasternTimeInstant(2025, Month.JULY, 29, 3, 0)
        val end = EasternTimeInstant(2025, Month.JULY, 30, 2, 59)
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

        composeTestRule.onNodeWithText("Tuesday, Jul 29, start of service").assertIsDisplayed()
        composeTestRule.onNodeWithText("3:00", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Tuesday, Jul 29, end of service").assertIsDisplayed()
        composeTestRule.onNodeWithText("2:59", substring = true).assertDoesNotExist()
    }

    @Test
    fun testLaterTodayActivePeriod() {
        val objects = ObjectCollectionBuilder()

        val now = EasternTimeInstant(2025, Month.JULY, 29, 9, 30)
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

        composeTestRule.onNodeWithText("9:10", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("11:30", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("later today", substring = true).assertIsDisplayed()
    }

    @Test
    fun testNoCurrentActivePeriod() {
        val objects = ObjectCollectionBuilder()

        val now = EasternTimeInstant.now()

        val alert =
            objects.alert {
                activePeriod(now - 10.minutes, now - 5.minutes)
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

        val now = EasternTimeInstant.now()

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

        val now = EasternTimeInstant.now()

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

        val now = EasternTimeInstant.now()

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

        val now = EasternTimeInstant.now()

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
