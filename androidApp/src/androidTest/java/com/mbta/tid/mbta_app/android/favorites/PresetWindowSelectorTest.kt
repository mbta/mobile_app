package com.mbta.tid.mbta_app.android.favorites

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.model.FavoriteSettings.Notifications.Window
import com.mbta.tid.mbta_app.model.PresetSelection
import com.mbta.tid.mbta_app.model.PresetWindow
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.assertEquals
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test

class PresetWindowSelectorTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testPresetWindowsVisible() {
        var selectedWindow: Window? = null
        composeTestRule.setContent {
            PresetWindowSelector(
                presetRows =
                    listOf(
                        listOf(
                            PresetWindow(
                                window = Window(Window.morningDefaultTimes),
                                label = "Morning",
                            )
                        ),
                        listOf(
                            PresetWindow(
                                window = Window(Window.middayDefaultTimes),
                                label = "Midday",
                            )
                        ),
                    ),
                selectedPreset = PresetSelection.Preset(rowIndex = 1, columnIndex = 0),
                presetsEnabled = true,
                onSelect = { window -> selectedWindow = window },
                now = EasternTimeInstant(LocalDateTime(2026, 8, 27, 4, 30, 0)),
            )
        }

        composeTestRule.onNodeWithText("Morning").assertIsNotSelected()
        composeTestRule.onNodeWithText("Midday").assertIsSelected()
        composeTestRule.onNodeWithText("Custom").assertIsNotSelected()

        composeTestRule.onNodeWithText("Morning").performClick()
        assertEquals(Window(Window.morningDefaultTimes), selectedWindow)
    }

    @Test
    fun testPresetButtonsDisabled() {
        composeTestRule.setContent {
            PresetWindowSelector(
                presetRows =
                    listOf(
                        listOf(PresetWindow(window = Window.morningDefault, label = "Morning")),
                        listOf(PresetWindow(window = Window.middayDefault, label = "Midday")),
                    ),
                selectedPreset = PresetSelection.Preset(rowIndex = 1, columnIndex = 0),
                presetsEnabled = false,
                onSelect = {},
            )
        }

        composeTestRule.onNodeWithText("Morning").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Midday").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Custom").assertIsEnabled()
    }

    @Test
    fun testCustomDefaultsToNow() {
        var selectedWindow: Window? = null
        composeTestRule.setContent {
            PresetWindowSelector(
                presetRows =
                    listOf(
                        listOf(PresetWindow(window = Window.morningDefault, label = "Morning")),
                        listOf(PresetWindow(window = Window.middayDefault, label = "Midday")),
                    ),
                selectedPreset = PresetSelection.Preset(rowIndex = 1, columnIndex = 0),
                presetsEnabled = true,
                onSelect = { window -> selectedWindow = window },
                now = EasternTimeInstant(LocalDateTime(2026, 8, 27, 4, 30, 0)),
            )
        }

        composeTestRule.onNodeWithText("Custom").performClick()
        assertEquals(
            selectedWindow,
            Window(
                startTime = LocalTime(4, 0, 0),
                endTime = LocalTime(5, 0, 0),
                daysOfWeek =
                    setOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY,
                    ),
            ),
        )
    }

    @Test
    fun testCustomDefaultsToNowLateNight() {
        var selectedWindow: Window? = null
        composeTestRule.setContent {
            PresetWindowSelector(
                presetRows =
                    listOf(
                        listOf(PresetWindow(window = Window.morningDefault, label = "Morning")),
                        listOf(PresetWindow(window = Window.middayDefault, label = "Midday")),
                    ),
                selectedPreset = PresetSelection.Preset(rowIndex = 1, columnIndex = 0),
                presetsEnabled = true,
                onSelect = { window -> selectedWindow = window },
                now = EasternTimeInstant(LocalDateTime(2026, 8, 27, 23, 30, 0)),
            )
        }

        composeTestRule.onNodeWithText("Custom").performClick()
        assertEquals(
            selectedWindow,
            Window(
                startTime = LocalTime(23, 0, 0),
                endTime = LocalTime(23, 59, 0),
                daysOfWeek =
                    setOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY,
                    ),
            ),
        )
    }
}
