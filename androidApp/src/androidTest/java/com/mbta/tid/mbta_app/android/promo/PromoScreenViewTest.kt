package com.mbta.tid.mbta_app.android.promo

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.testUtils.assertCanBeDisplayed
import com.mbta.tid.mbta_app.model.FeaturePromo
import kotlin.test.assertTrue
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test

class PromoScreenViewTest {

    @get:Rule val composeTestRule = createComposeRule(effectContext = UnconfinedTestDispatcher())

    @Test
    fun testEnhancedFavorites() {
        var calledOnAdvance = false
        composeTestRule.setContent {
            PromoScreenView(FeaturePromo.EnhancedFavorites) { calledOnAdvance = true }
        }
        composeTestRule.onNodeWithText("Add your favorites").assertCanBeDisplayed()
        composeTestRule
            .onNodeWithText("Now save your frequently used stops", substring = true)
            .assertCanBeDisplayed()

        composeTestRule.onNodeWithText("Got it").performClick()

        assertTrue(calledOnAdvance)
    }
}
