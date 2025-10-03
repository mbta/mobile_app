package com.mbta.tid.mbta_app.android.favorites

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.mbta.tid.mbta_app.model.FavoriteSettings
import kotlin.test.assertEquals
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test

class NotificationSettingsWidgetTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testAddTimePeriod() {
        lateinit var settings: MutableState<FavoriteSettings.Notifications>
        composeTestRule.setContent {
            settings = remember { mutableStateOf(FavoriteSettings.Notifications.disabled) }
            var settings by settings
            NotificationSettingsWidget(settings, setSettings = { settings = it })
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        assertEquals(1, settings.value.windows.size)
        composeTestRule.onNodeWithText("08:00\u202fAM").assertIsDisplayed()
        composeTestRule.onNodeWithText("09:00\u202fAM").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sun").assertIsOff()
        composeTestRule.onNodeWithText("Mon").assertIsOn()
        composeTestRule.onNodeWithText("Tue").assertIsOn()
        composeTestRule.onNodeWithText("Wed").assertIsOn()
        composeTestRule.onNodeWithText("Thu").assertIsOn()
        composeTestRule.onNodeWithText("Fri").assertIsOn()
        composeTestRule.onNodeWithText("Sat").assertIsOff()
        composeTestRule.onNodeWithContentDescription("Delete").assertDoesNotExist()
        composeTestRule.onNodeWithText("Add another time period").performClick()
        assertEquals(2, settings.value.windows.size)
        composeTestRule.onNodeWithText("12:00\u202fPM").assertIsDisplayed()
        composeTestRule.onNodeWithText("01:00\u202fPM").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Sun").onLast().assertIsOn()
        composeTestRule.onAllNodesWithText("Mon").onLast().assertIsOff()
        composeTestRule.onAllNodesWithText("Tue").onLast().assertIsOff()
        composeTestRule.onAllNodesWithText("Wed").onLast().assertIsOff()
        composeTestRule.onAllNodesWithText("Thu").onLast().assertIsOff()
        composeTestRule.onAllNodesWithText("Fri").onLast().assertIsOff()
        composeTestRule.onAllNodesWithText("Sat").onLast().assertIsOn()
        composeTestRule.onAllNodesWithContentDescription("Delete").assertCountEquals(2)
    }

    @Test
    fun testChangeTime() {
        lateinit var settings: MutableState<FavoriteSettings.Notifications>
        composeTestRule.setContent {
            settings = remember { mutableStateOf(FavoriteSettings.Notifications.disabled) }
            var settings by settings
            NotificationSettingsWidget(settings, setSettings = { settings = it })
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        composeTestRule.onNodeWithText("08:00\u202fAM").performClick()
        composeTestRule.onNodeWithContentDescription("for hour").performTextReplacement("7")
        composeTestRule.onNodeWithContentDescription("for minutes").performTextReplacement("45")
        composeTestRule.onNodeWithText("Okay").performClick()
        assertEquals(LocalTime(7, 45), settings.value.windows.single().startTime)
        composeTestRule.onNodeWithText("09:00\u202fAM").performClick()
        composeTestRule.onNodeWithContentDescription("for hour").performTextReplacement("9")
        composeTestRule.onNodeWithContentDescription("for minutes").performTextReplacement("10")
        composeTestRule.onNodeWithText("Okay").performClick()
        assertEquals(LocalTime(9, 10), settings.value.windows.single().endTime)
    }

    @Test
    fun testChangeDays() {
        lateinit var settings: MutableState<FavoriteSettings.Notifications>
        composeTestRule.setContent {
            settings = remember { mutableStateOf(FavoriteSettings.Notifications.disabled) }
            var settings by settings
            NotificationSettingsWidget(settings, setSettings = { settings = it })
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        composeTestRule.onNodeWithText("Sun").performClick()
        composeTestRule.onNodeWithText("Wed").performClick()
        composeTestRule.onNodeWithText("Fri").performClick()
        assertEquals(
            setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
            settings.value.windows.single().daysOfWeek,
        )
    }
}
