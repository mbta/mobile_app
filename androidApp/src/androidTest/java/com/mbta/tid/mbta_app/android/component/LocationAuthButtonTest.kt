package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.location.MockLocationDataManager
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlin.time.Duration.Companion.seconds
import org.junit.Rule
import org.junit.Test

class LocationAuthButtonTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testShowsWhenPermissionsNotGranted() {

        val locationManager = MockLocationDataManager()
        locationManager.hasPermission = false

        composeTestRule.setContent { LocationAuthButton(locationDataManager = locationManager) }

        composeTestRule.onNodeWithText("Location Services is off").assertIsDisplayed()
    }

    @Test
    fun testHiddenWhenHasPermissions() {

        val locationManager = MockLocationDataManager()
        locationManager.hasPermission = true

        composeTestRule.setContent { LocationAuthButton(locationDataManager = locationManager) }

        composeTestRule.onNodeWithText("Location Services is off").assertDoesNotExist()
    }

    @Test
    fun testShowsSettingsPromptWhenPermanentlyDeniedBefore() {

        val requestStartTime = EasternTimeInstant.now()
        val requestEndTime = requestStartTime.plus(0.1.seconds)

        assertTrue(shouldShowSettingsPrompt(false, requestStartTime, requestEndTime))
    }

    @Test
    fun testDoesntShowsSettingsPromptWhenPermanentlyDeniedJustNow() {

        val requestStartTime = EasternTimeInstant.now()
        val requestEndTime = requestStartTime.plus(0.36.seconds)

        assertFalse(shouldShowSettingsPrompt(false, requestStartTime, requestEndTime))
    }

    @Test
    fun testDoesntShowsSettingsPromptWhenShouldShowRationale() {

        val requestStartTime = EasternTimeInstant.now()
        val requestEndTime = requestStartTime.plus(0.1.seconds)

        assertFalse(shouldShowSettingsPrompt(true, requestStartTime, requestEndTime))
    }
}
