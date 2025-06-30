package com.mbta.tid.mbta_app.android.map

import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mbta.tid.mbta_app.android.location.MockLocationDataManager
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import io.github.dellisd.spatialk.geojson.Position
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test

class MapViewModelTests {

    @Test
    fun testUpdateCenterButtonVisibilityWhenLocationKnownAndNotFollowing() = runBlocking {
        val mapViewModel = MapViewModel()

        assertEquals(false, mapViewModel.showRecenterButton.value)

        val locationDataManager = MockLocationDataManager()
        locationDataManager.hasPermission = true
        val viewportProvider = ViewportProvider(MapViewportState())
        viewportProvider.setIsManuallyCentering(true)

        mapViewModel.updateCenterButtonVisibility(
            MockLocationDataManager.MockLocation(Position(0.0, 0.0)),
            locationDataManager,
            false,
            viewportProvider,
        )

        assertEquals(true, mapViewModel.showRecenterButton.value)
    }
}
