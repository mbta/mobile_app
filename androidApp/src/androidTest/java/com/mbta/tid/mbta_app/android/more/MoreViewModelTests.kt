package com.mbta.tid.mbta_app.android.more

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class MoreViewModelTests {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testToggleSetting() = runTest {
        var toggledHideMaps = false

        val settingsRepository =
            MockSettingsRepository(
                settings = mapOf(Settings.HideMaps to false),
                onSaveSettings = { settings ->
                    if (settings.size == 1 && settings.containsKey(Settings.HideMaps)) {
                        toggledHideMaps = true
                    }
                }
            )

        var vm: MoreViewModel? = null
        composeTestRule.setContent { vm = MoreViewModel(LocalContext.current, settingsRepository) }

        composeTestRule.awaitIdle()
        vm!!.toggleSetting(Settings.HideMaps)
        composeTestRule.awaitIdle()
        composeTestRule.waitUntil { toggledHideMaps }

        assertTrue { toggledHideMaps }
    }
}
