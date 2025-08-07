package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.graphics.Color
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
        composeTestRule.setContent {
            StarButton(starred = false, color = Color.Unspecified) { wasTapped = true }
        }

        composeTestRule.onNodeWithContentDescription("Star route").performClick()
        assertTrue(wasTapped)
    }
}
