package com.mbta.tid.mbta_app.android.util

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import kotlin.test.assertEquals
import kotlin.test.fail
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.koin.compose.koinInject

class SettingsCacheTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testLoadsSettings() {
        var gotSettings = false
        val settingsRepo =
            MockSettingsRepository(
                mapOf(Settings.HideMaps to true),
                onGetSettings = { gotSettings = true },
            )
        loadKoinMocks { settings = settingsRepo }

        val hideMapsValues = mutableListOf<Boolean>()
        val debugModeValues = mutableListOf<Boolean>()
        composeTestRule.setContent {
            val cache: SettingsCache = koinInject()
            val hideMaps = cache.get(Settings.HideMaps)
            hideMapsValues.add(hideMaps)
            val debugMode = cache.get(Settings.DevDebugMode)
            debugModeValues.add(debugMode)
        }

        composeTestRule.waitForIdle()
        composeTestRule.waitUntil { gotSettings }
        assertEquals(listOf(false, true), hideMapsValues)
        assertEquals(listOf(false, false), debugModeValues)
    }

    @Test
    fun testUsesCachedSettings() {
        var gotSettings by mutableStateOf(false)
        val settingsRepo =
            MockSettingsRepository(
                mapOf(Settings.HideMaps to true),
                onGetSettings = { gotSettings = true },
            )
        loadKoinMocks { settings = settingsRepo }

        val hideMapsValues = mutableListOf<Boolean>()
        composeTestRule.setContent {
            val cache: SettingsCache = koinInject()
            cache.get(Settings.StationAccessibility)
            if (gotSettings) {
                val hideMaps = cache.get(Settings.HideMaps)
                hideMapsValues.add(hideMaps)
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.waitUntil { gotSettings }
        assertEquals(listOf(true), hideMapsValues)
    }

    @Test
    fun testSavesSettings() {
        var gotSettings = false
        var savedSettings = false
        val settingsRepo =
            MockSettingsRepository(
                settings = mapOf(Settings.HideMaps to false),
                onGetSettings = { gotSettings = true },
                onSaveSettings = { newSettings ->
                    assertEquals(mapOf(Settings.HideMaps to true), newSettings)
                    savedSettings = true
                },
            )
        loadKoinMocks { settings = settingsRepo }

        val hideMapsValues = mutableListOf<Boolean>()
        composeTestRule.setContent {
            val cache: SettingsCache = koinInject()
            val hideMaps = cache.get(Settings.HideMaps)
            hideMapsValues.add(hideMaps)
            Button({ cache.set(Settings.HideMaps, true) }) { Text("Hide maps") }
        }

        composeTestRule.waitForIdle()
        composeTestRule.waitUntil { gotSettings }
        composeTestRule.onNodeWithText("Hide maps").performClick()
        composeTestRule.waitUntil { savedSettings }
        composeTestRule.waitForIdle()
        // false while loading and still false after loading
        assertEquals(listOf(false, false, true), hideMapsValues)
    }

    @Test
    @Ignore("No overriden settings right now - typically only before cutting over feature flag")
    fun testOverridesSettings() {
        val settingsRepo =
            MockSettingsRepository(
                mapOf(Settings.HideMaps to false),
                onGetSettings = { fail("Should not be getting settings from repo") },
            )
        loadKoinMocks { settings = settingsRepo }

        val settingValues = mutableListOf<Boolean>()
        composeTestRule.setContent {
            val cache: SettingsCache = koinInject()
            settingValues.add(cache.get(Settings.HideMaps))
            cache.set(Settings.HideMaps, true)
            settingValues.add(cache.get(Settings.HideMaps))
        }

        composeTestRule.waitForIdle()
        assertEquals(listOf(false, true), settingValues)
    }
}
