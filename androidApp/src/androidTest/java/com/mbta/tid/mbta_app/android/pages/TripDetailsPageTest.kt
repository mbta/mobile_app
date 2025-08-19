package com.mbta.tid.mbta_app.android.pages

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class TripDetailsPageTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testHasCloseButton() {
        var onCloseCalled = false
        composeTestRule.setContent {
            TripDetailsPage(
                filter = TripDetailsPageFilter("tripId", "vehicleId", "routeId", 0, "stopId", null),
                onClose = { onCloseCalled = true },
            )
        }

        composeTestRule.onNodeWithContentDescription("Close").performClick()
        assertTrue(onCloseCalled)
    }
}
