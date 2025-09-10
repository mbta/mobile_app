package com.mbta.tid.mbta_app.android.more

import android.annotation.SuppressLint
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.morePage.MoreItem
import com.mbta.tid.mbta_app.model.morePage.MoreSection
import java.util.Locale
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class MoreViewModelTests {

    @get:Rule val composeTestRule = createComposeRule()

    @SuppressLint("LocalContextConfigurationRead")
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

            vm = MoreViewModel(subContext, {})
        }

        composeTestRule.waitUntilDefaultTimeout {
            vm.sections.value.any { it.id == MoreSection.Category.Feedback }
        }
        assertEquals(
            "https://mbta.com/androidappfeedback?lang=es-US",
            vm.sections.value
                .first { it.id == MoreSection.Category.Feedback }
                .items
                .filterIsInstance<MoreItem.Link>()
                .first()
                .url,
        )
    }
}
