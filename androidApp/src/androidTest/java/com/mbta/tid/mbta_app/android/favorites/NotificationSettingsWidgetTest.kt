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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.mbta.tid.mbta_app.android.hasTextMatching
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
        composeTestRule.onNode(hasTextMatching(Regex("08:00\\sAM"))).performScrollTo().assertIsDisplayed()
        composeTestRule.onNode(hasTextMatching(Regex("09:00\\sAM"))).assertIsDisplayed()
        composeTestRule.onNodeWithText("Sunday").assertIsOff()
        composeTestRule.onNodeWithText("Monday").assertIsOn()
        composeTestRule.onNodeWithText("Tuesday").assertIsOn()
        composeTestRule.onNodeWithText("Wednesday").assertIsOn()
        composeTestRule.onNodeWithText("Thursday").assertIsOn()
        composeTestRule.onNodeWithText("Friday").assertIsOn()
        composeTestRule.onNodeWithText("Saturday").assertIsOff()
        composeTestRule.onNodeWithContentDescription("Delete").assertDoesNotExist()
        composeTestRule.onNodeWithText("Add another time period").performClick()
        assertEquals(2, settings.value.windows.size)
        composeTestRule.onNode(hasTextMatching(Regex("12:00\\sPM"))).assertIsDisplayed()
        composeTestRule.onNode(hasTextMatching(Regex("01:00\\sPM"))).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Sunday").onLast().assertIsOn()
        composeTestRule.onAllNodesWithText("Monday").onLast().assertIsOff()
        composeTestRule.onAllNodesWithText("Tuesday").onLast().assertIsOff()
        composeTestRule.onAllNodesWithText("Wednesday").onLast().assertIsOff()
        composeTestRule.onAllNodesWithText("Thursday").onLast().assertIsOff()
        composeTestRule.onAllNodesWithText("Friday").onLast().assertIsOff()
        composeTestRule.onAllNodesWithText("Saturday").onLast().assertIsOn()
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
        composeTestRule.onNode(hasTextMatching(Regex("08:00\\sAM"))).performClick()
        composeTestRule.onNodeWithContentDescription("for hour").performTextReplacement("7")
        composeTestRule.onNodeWithContentDescription("for minutes").performTextReplacement("45")
        composeTestRule.onNodeWithText("Okay").performClick()
        assertEquals(LocalTime(7, 45), settings.value.windows.single().startTime)
        composeTestRule.onNode(hasTextMatching(Regex("09:00\\sAM"))).performClick()
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
        composeTestRule.onNodeWithText("Sunday").performClick()
        composeTestRule.onNodeWithText("Wednesday").performClick()
        composeTestRule.onNodeWithText("Friday").performClick()
        assertEquals(
            setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
            settings.value.windows.single().daysOfWeek,
        )
    }
}
