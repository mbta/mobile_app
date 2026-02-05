package com.mbta.tid.mbta_app.android.pages

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.location.MockLocationDataManager
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.OnboardingScreen
import com.mbta.tid.mbta_app.repositories.MockOnboardingRepository
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingPageTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testFlow() {
        val completedScreens = mutableSetOf<OnboardingScreen>()
        val onboardingRepository =
            MockOnboardingRepository(
                pendingOnboarding = OnboardingScreen.entries,
                onMarkComplete = completedScreens::add,
            )
        var finished = false

        composeTestRule.setContent {
            OnboardingPage(
                screens = OnboardingScreen.entries,
                locationDataManager = MockLocationDataManager(),
                onFinish = { finished = true },
                onboardingRepository = onboardingRepository,
                skipLocationDialogue = true,
            )
        }

        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.waitUntilDefaultTimeout { completedScreens.size == 1 }
        assertEquals(1, completedScreens.size)

        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.waitUntilDefaultTimeout { completedScreens.size == 2 }
        assertEquals(2, completedScreens.size)

        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.waitUntilDefaultTimeout { completedScreens.size == 3 }
        assertEquals(3, completedScreens.size)

        composeTestRule.onNodeWithText("Got it").performClick()
        composeTestRule.waitUntilDefaultTimeout { completedScreens.size == 4 }
        assertEquals(4, completedScreens.size)

        composeTestRule.onNodeWithText("Get started").performClick()
        composeTestRule.waitUntilDefaultTimeout { completedScreens.size == 5 }
        assertEquals(OnboardingScreen.entries.toSet(), completedScreens)
        assertTrue(finished)
    }
}
