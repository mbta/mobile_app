package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.testUtils.assertCanBeDisplayed
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class NoNearbyStopsViewTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun displaysText() {
        composeTestRule.setContent {
            NoNearbyStopsView(onOpenSearch = {}, onPanToDefaultCenter = {})
        }

        composeTestRule.onNodeWithText("No nearby stops").assertCanBeDisplayed()
        composeTestRule
            .onNodeWithText("You’re outside the MBTA service area.")
            .assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Search by stop").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("View transit near Boston").assertCanBeDisplayed()
    }

    @Test
    fun buttonsWork() {
        var openedSearch = false
        var pannedToDefaultCenter = false
        composeTestRule.setContent {
            NoNearbyStopsView(
                onOpenSearch = { openedSearch = true },
                onPanToDefaultCenter = { pannedToDefaultCenter = true },
            )
        }

        composeTestRule.onNodeWithText("Search by stop").performClick()
        assertTrue(openedSearch)
        composeTestRule.onNodeWithText("View transit near Boston").performClick()
        assertTrue(pannedToDefaultCenter)
    }
}
