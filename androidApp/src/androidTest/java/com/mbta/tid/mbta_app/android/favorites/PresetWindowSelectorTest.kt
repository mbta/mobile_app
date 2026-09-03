package com.mbta.tid.mbta_app.android.favorites

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.model.Preset
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class PresetWindowSelectorTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testPresetWindowsVisible() {
        var selectedPreset: Preset? = null
        composeTestRule.setContent {
            PresetWindowSelector(
                presetRows =
                    listOf(
                        listOf(Preset.Morning),
                        listOf(Preset.Midday),
                    ),
                selectedPreset = Preset.Midday,
                onSelect = { preset -> selectedPreset = preset },
            )
        }

        composeTestRule.onNodeWithText("Morning").assertIsNotSelected()
        composeTestRule.onNodeWithText("Midday").assertIsSelected()
        composeTestRule.onNodeWithText("Custom").assertIsNotSelected()

        composeTestRule.onNodeWithText("Morning").performClick()

        assertEquals(Preset.Morning, selectedPreset)
    }

    @Test
    fun testCustomSetsNullPreset() {
        var selectedPreset: Preset? = Preset.Midday
        composeTestRule.setContent {
            PresetWindowSelector(
                presetRows =
                    listOf(
                        listOf(Preset.Morning),
                        listOf(Preset.Midday),
                    ),
                selectedPreset = Preset.Midday,
                onSelect = { preset -> selectedPreset = preset },
            )
        }

        composeTestRule.onNodeWithText("Custom").performClick()
        assertEquals(
            null,
            selectedPreset,
        )
    }
}
