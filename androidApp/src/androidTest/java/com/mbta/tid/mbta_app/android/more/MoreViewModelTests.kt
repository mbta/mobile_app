package com.mbta.tid.mbta_app.android.more

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.model.morePage.MoreItem
import com.mbta.tid.mbta_app.model.morePage.MoreSection
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class MoreViewModelTests {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testToggleSetting() {
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
        composeTestRule.setContent {
            vm = MoreViewModel(LocalContext.current, {}, settingsRepository)
        }

        composeTestRule.waitForIdle()
        vm!!.toggleSetting(Settings.HideMaps)
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil { toggledHideMaps }

        assertTrue { toggledHideMaps }
    }

    @Test
    fun testLocalizedFeedbackLink() {
        lateinit var vm: MoreViewModel
        composeTestRule.setContent {
            val context = LocalContext.current

            // https://stackoverflow.com/a/21810126
            val resources = context.resources
            val config = resources.configuration
            config.setLocale(Locale.forLanguageTag("es-MX"))
            val subContext = context.createConfigurationContext(config)

            vm = MoreViewModel(subContext, {}, MockSettingsRepository())
        }

        composeTestRule.waitUntil {
            vm.sections.value.any { it.id == MoreSection.Category.Feedback }
        }
        assertEquals(
            "https://mbta.com/androidappfeedback?lang=es-US",
            vm.sections.value
                .first { it.id == MoreSection.Category.Feedback }
                .items
                .filterIsInstance<MoreItem.Link>()
                .first()
                .url
        )
    }
}
