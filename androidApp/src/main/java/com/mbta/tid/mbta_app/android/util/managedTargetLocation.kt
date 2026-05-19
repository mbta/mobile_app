package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mbta.tid.mbta_app.android.pages.NearbyTransit
import org.maplibre.spatialk.geojson.Position

@Composable
fun managedTargetLocation(nearbyTransit: NearbyTransit, reset: () -> Unit = {}): State<Position?> {
    val position = remember { mutableStateOf<Position?>(null) }

    val location by nearbyTransit.locationDataManager.currentLocation.collectAsStateWithLifecycle()
    LaunchedEffect(location) {
        if (
            nearbyTransit.viewportProvider.isFollowingPuck &&
                !nearbyTransit.viewportProvider.isManuallyCentering
        ) {
            position.value = location?.let { Position(it.longitude, it.latitude) }
        }
    }

    val cameraState by
        nearbyTransit.viewportProvider.cameraStateFlowDebounced.collectAsStateWithLifecycle(null)
    LaunchedEffect(cameraState) {
        cameraState?.let {
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
