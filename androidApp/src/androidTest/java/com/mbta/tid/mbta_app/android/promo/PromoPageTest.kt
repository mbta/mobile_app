package com.mbta.tid.mbta_app.android.promo

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.model.FeaturePromo
import org.junit.Rule
import org.junit.Test

class PromoPageTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testFlow() {
        var calledOnAdvance = false
        var calledFinish = false

        composeTestRule.setContent {
            PromoPage(
                listOf(FeaturePromo.CombinedStopAndTrip, FeaturePromo.EnhancedFavorites),
                { calledFinish = true },
            ) {
                calledOnAdvance = true
            }
        }

        composeTestRule.onNodeWithText("Got it").performClick()

        composeTestRule.waitUntil { calledOnAdvance && calledFinish }
    }
}
