package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.util.formattedServiceDay
import com.mbta.tid.mbta_app.android.util.formattedTime
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.Facility
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.utils.serviceDate
import com.mbta.tid.mbta_app.utils.toBostonTime
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import org.junit.Rule
import org.junit.Test

class AlertCardTests {

    @get:Rule val composeTestRule = createComposeRule()

    private val color = Color.fromHex("ED8B00")
    private val textColor = Color.fromHex("FFFFFF")

    @Test
    fun testDownstreamAlertCard() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Detour
            }
        var onViewDetailsClicked = false
        composeTestRule.setContent {
            AlertCard(
                alert,
                null,
                AlertCardSpec.Downstream,
                color,
                textColor,
                { onViewDetailsClicked = true },
            )
        }

        composeTestRule.onNodeWithText("Detour ahead").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Alert header").assertIsNotDisplayed()
        assertTrue { onViewDetailsClicked }
    }

    @Test
    fun testMajorAlertCard() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Suspension
            }
        var onViewDetailsClicked = false
        composeTestRule.setContent {
            AlertCard(
                alert,
                null,
                AlertCardSpec.Major,
                color,
                textColor,
                { onViewDetailsClicked = true },
            )
        }

        composeTestRule.onNodeWithText("Suspension").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alert header").assertIsDisplayed()
        composeTestRule.onNodeWithText("View details").performClick()
        assertTrue { onViewDetailsClicked }
    }

    @Test
    fun testSecondaryAlertCard() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Detour
            }
        var onViewDetailsClicked = false
        composeTestRule.setContent {
            AlertCard(
                alert,
                null,
                AlertCardSpec.Secondary,
                color,
                textColor,
                { onViewDetailsClicked = true },
            )
        }

        composeTestRule.onNodeWithText("Detour").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Alert header").assertIsNotDisplayed()
        assertTrue { onViewDetailsClicked }
    }

    @Test
    fun testElevatorAlertCard() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.ElevatorClosure
            }
        var onViewDetailsClicked = false
        composeTestRule.setContent {
            AlertCard(
                alert,
                null,
                AlertCardSpec.Elevator,
                color,
                textColor,
                { onViewDetailsClicked = true },
            )
        }

        composeTestRule.onNodeWithText("Alert header").assertIsDisplayed().performClick()
        assertTrue { onViewDetailsClicked }
    }

    @Test
    fun testElevatorAlertWithFacilityCard() {
        val facility =
            ObjectCollectionBuilder.Single.facility {
                type = Facility.Type.Elevator
                shortName = "Elevator name"
            }
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.ElevatorClosure
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.UsingWheelchair),
                    facility = facility.id,
                )
                facilities = mapOf(facility.id to facility)
            }
        var onViewDetailsClicked = false
        composeTestRule.setContent {
            AlertCard(
                alert,
                null,
                AlertCardSpec.Elevator,
                color,
                textColor,
                { onViewDetailsClicked = true },
            )
        }

        composeTestRule
            .onNodeWithText("Elevator closure (Elevator name)")
            .assertIsDisplayed()
            .performClick()
        assertTrue { onViewDetailsClicked }
    }

    @Test
    fun testDelayAlertCard() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Delay
                cause = Alert.Cause.HeavyRidership
            }
        composeTestRule.setContent {
            AlertCard(alert, null, AlertCardSpec.Delay, color, textColor, {})
        }

        composeTestRule.onNodeWithText("Delays due to heavy ridership").assertIsDisplayed()
    }

    @Test
    fun testDelayAlertCardUnknownCause() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Delay
                cause = Alert.Cause.UnknownCause
            }
        composeTestRule.setContent {
            AlertCard(alert, null, AlertCardSpec.Delay, color, textColor, {})
        }

        composeTestRule.onNodeWithText("Delays").assertIsDisplayed()
    }

    @Test
    fun testSingleTrackingInfoDelay() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Delay
                cause = Alert.Cause.SingleTracking
                severity = 1
            }
        composeTestRule.setContent {
            AlertCard(alert, null, AlertCardSpec.Delay, color, textColor, {})
        }

        composeTestRule.onNodeWithText("Single Tracking").assertIsDisplayed()
    }

    @Test
    fun testMajorAlertCardSummary() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Suspension
            }

        // Fixed time so we can have a specific day of the week (wed)
        val now = Instant.fromEpochMilliseconds(1743598800000)

        val saturday = now.toBostonTime().serviceDate.plus(DatePeriod(days = 3))
        val serviceEndTime = LocalTime(hour = 5, minute = 0)
        val endTime = saturday.atTime(serviceEndTime).toInstant(TimeZone.of("America/New_York"))
        composeTestRule.setContent {
            AlertCard(
                alert,
                AlertSummary(alert.effect, null, AlertSummary.Timeframe.ThisWeek(endTime)),
                AlertCardSpec.Major,
                color,
                textColor,
                {},
            )
        }

        composeTestRule.onNodeWithText("Suspension").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Service suspended through ${endTime.formattedServiceDay()}")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Alert header").assertDoesNotExist()
        composeTestRule.onNodeWithText("View details").performClick()
    }

    @Test
    fun testSecondaryAlertCardSummary() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Detour
            }
        val endTime = Instant.fromEpochMilliseconds(1743629400000)
        composeTestRule.setContent {
            AlertCard(
                alert,
                AlertSummary(alert.effect, null, AlertSummary.Timeframe.Time(endTime)),
                AlertCardSpec.Secondary,
                color,
                textColor,
                {},
            )
        }

        composeTestRule
            .onNodeWithText("Detour through ${endTime.formattedTime()}")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Alert header").assertIsNotDisplayed()
    }
}
