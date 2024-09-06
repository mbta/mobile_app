package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import io.github.dellisd.spatialk.geojson.Position
import org.junit.Rule
import org.junit.Test

class NearbyTransitViewTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testEmpty() {
        val objects = ObjectCollectionBuilder()
        composeTestRule.setContent {
            NearbyTransitView(
                alertData = null,
                globalResponse = GlobalResponse(objects, emptyMap()),
                targetLocation = Position(0.0, 0.0),
                setLastLocation = {},
                onOpenStopDetails = { _, _ -> }
            )
        }
        composeTestRule.onNodeWithText("No nearby MBTA stops").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Your current location is outside of our search area.")
            .assertIsDisplayed()
    }
}
