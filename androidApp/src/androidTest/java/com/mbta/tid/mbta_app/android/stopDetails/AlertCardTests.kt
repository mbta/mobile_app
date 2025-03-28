package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import kotlin.test.assertTrue
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
                { onViewDetailsClicked = true }
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
                { onViewDetailsClicked = true }
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
                { onViewDetailsClicked = true }
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
                { onViewDetailsClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Alert header").assertIsDisplayed().performClick()
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
}
