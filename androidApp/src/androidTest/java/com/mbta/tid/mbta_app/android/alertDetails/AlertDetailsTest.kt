package com.mbta.tid.mbta_app.android.alertDetails

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
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
                stopId = null,
                affectedStops = listOf(stop1, stop2, stop3),
                now = now
            )
        }

        composeTestRule.onNodeWithText("Red Line Stop Closure").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unruly Passenger").assertIsDisplayed()
        composeTestRule.onNodeWithText(alert.activePeriod[0].formatStart().text).assertIsDisplayed()
        composeTestRule.onNodeWithText("3 affected stops").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 affected stops").performClick()
        composeTestRule.onNodeWithText(stop1.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop2.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop3.name).assertIsDisplayed()
        composeTestRule.onNodeWithText("3 affected stops").performClick()
        composeTestRule.onNodeWithText("Full Description").assertIsDisplayed()
        composeTestRule.onNodeWithText(alert.description!!).assertIsDisplayed()
        composeTestRule.onNodeWithText(alert.header!!).assertIsDisplayed()
        composeTestRule.onNodeWithText("Updated: 1/22/25, 10:36 AM").assertIsDisplayed()
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
                stopId = null,
                affectedStops = emptyList(),
                now = now
            )
        }

        composeTestRule.onNodeWithText(alert.activePeriod[1].formatStart().text).assertIsDisplayed()
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
                stopId = null,
                affectedStops = emptyList(),
                now = now
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
                stopId = null,
                affectedStops = emptyList(),
                now = now
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
                stopId = null,
                affectedStops = affectedStops.value,
                now = now
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
}
