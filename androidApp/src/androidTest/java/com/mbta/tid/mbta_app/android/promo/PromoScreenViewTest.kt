package com.mbta.tid.mbta_app.android.promo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.model.FeaturePromo
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class PromoScreenViewTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testEnhancedFavorites() {
        var calledOnAdvance = false
        composeTestRule.setContent {
            PromoScreenView(FeaturePromo.EnhancedFavorites) { calledOnAdvance = true }
        }
        composeTestRule.onNodeWithText("Add your favorites").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Now save your frequently used stops", substring = true)
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Got it").performClick()

        assertTrue(calledOnAdvance)
    }
}
