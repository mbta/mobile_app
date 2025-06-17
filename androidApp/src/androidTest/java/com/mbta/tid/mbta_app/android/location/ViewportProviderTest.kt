package com.mbta.tid.mbta_app.android.location

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.createComposeRule
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ViewportProviderTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testFollowUpdatesState() = runBlocking {
        val viewportProvider = ViewportProvider(MapViewportState())
        viewportProvider.isFollowingPuck = false
        viewportProvider.isVehicleOverview = true

        composeTestRule.setContent { LaunchedEffect(Unit) { viewportProvider.follow() } }
        composeTestRule.waitUntil { viewportProvider.isFollowingPuck }
        assertTrue(viewportProvider.isFollowingPuck)
        assertFalse(viewportProvider.isVehicleOverview)
    }
}
