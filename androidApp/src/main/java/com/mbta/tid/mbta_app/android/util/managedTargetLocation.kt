package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.android.pages.NearbyTransit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.debounce
import org.maplibre.spatialk.geojson.Position

@Composable
fun managedTargetLocation(nearbyTransit: NearbyTransit, reset: () -> Unit = {}): State<Position?> {
    var position = remember { mutableStateOf<Position?>(null) }
    LaunchedEffect(nearbyTransit.locationDataManager) {
        nearbyTransit.locationDataManager.currentLocation.collect { location ->
            if (
                nearbyTransit.viewportProvider.isFollowingPuck &&
                    !nearbyTransit.viewportProvider.isManuallyCentering
            ) {
                position.value = location?.let { Position(it.longitude, it.latitude) }
            }
        }
    }
    LaunchedEffect(nearbyTransit.viewportProvider) {
        nearbyTransit.viewportProvider.cameraStateFlow.debounce(0.5.seconds).collect {
            // since this LaunchedEffect is cancelled when not on the page
            // we don't need to check
            if (!nearbyTransit.viewportProvider.isFollowingPuck) {
                position.value = it.center.toPosition()
            }
        }
    }
    LaunchedEffect(nearbyTransit.viewportProvider.isManuallyCentering) {
        if (nearbyTransit.viewportProvider.isManuallyCentering) {
            reset()
            position.value = null
        }
    }
    LaunchedEffect(nearbyTransit.viewportProvider.isFollowingPuck) {
        if (nearbyTransit.viewportProvider.isFollowingPuck) {
            reset()
            position.value =
                nearbyTransit.locationDataManager.currentLocation.value?.let {
                    Position(it.longitude, it.latitude)
                }
        }
    }

    return position
}
