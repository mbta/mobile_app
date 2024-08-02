package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PinButtonTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun callsAction() {
        var wasTapped = false
        composeTestRule.setContent { PinButton(pinned = false) { wasTapped = true } }

        composeTestRule.onNodeWithContentDescription("pin route").performClick()
        assertTrue(wasTapped)
    }
}
