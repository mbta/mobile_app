package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.mbta.tid.mbta_app.android.pages.NearbyTransit
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.debounce

@Composable
fun managedTargetLocation(
    nearbyTransit: NearbyTransit,
    updateTargetLocation: (Position?) -> Unit,
    reset: () -> Unit,
) {
    LaunchedEffect(nearbyTransit.locationDataManager) {
        nearbyTransit.locationDataManager.currentLocation.collect { location ->
            if (
                nearbyTransit.viewportProvider.isFollowingPuck &&
                    !nearbyTransit.viewportProvider.isManuallyCentering
            ) {
                updateTargetLocation(location?.let { Position(it.longitude, it.latitude) })
            }
        }
    }
    LaunchedEffect(nearbyTransit.viewportProvider) {
        nearbyTransit.viewportProvider.cameraStateFlow.debounce(0.5.seconds).collect {
            // since this LaunchedEffect is cancelled when not on the page
            // we don't need to check
            if (!nearbyTransit.viewportProvider.isFollowingPuck) {
                updateTargetLocation(it.center.toPosition())
            }
        }
    }
    LaunchedEffect(nearbyTransit.viewportProvider.isManuallyCentering) {
        if (nearbyTransit.viewportProvider.isManuallyCentering) {
            reset()
            updateTargetLocation(null)
        }
    }
    LaunchedEffect(nearbyTransit.viewportProvider.isFollowingPuck) {
        if (nearbyTransit.viewportProvider.isFollowingPuck) {
            reset()
            updateTargetLocation(
                nearbyTransit.locationDataManager.currentLocation.value?.let {
                    Position(it.longitude, it.latitude)
                }
            )
        }
    }
}
