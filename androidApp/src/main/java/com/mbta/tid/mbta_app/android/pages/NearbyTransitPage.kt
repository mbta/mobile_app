package com.mbta.tid.mbta_app.android.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitView
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitViewModel
import com.mbta.tid.mbta_app.android.nearbyTransit.NoNearbyStopsView
import com.mbta.tid.mbta_app.android.util.toPosition
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(FlowPreview::class)
@Composable
fun NearbyTransitPage(
    nearbyTransit: NearbyTransit,
    onOpenStopDetails: (String, StopDetailsFilter?) -> Unit,
    openSearch: () -> Unit,
    nearbyViewModel: NearbyTransitViewModel,
    errorBannerViewModel: ErrorBannerViewModel,
) {
    var targetLocation by remember { mutableStateOf<Position?>(null) }
    LaunchedEffect(nearbyTransit.locationDataManager) {
        nearbyTransit.locationDataManager.currentLocation.collect { location ->
            if (
                nearbyTransit.viewportProvider.isFollowingPuck &&
                    !nearbyTransit.viewportProvider.isManuallyCentering
            ) {
                targetLocation = location?.let { Position(it.longitude, it.latitude) }
            }
        }
    }
    LaunchedEffect(nearbyTransit.viewportProvider) {
        nearbyTransit.viewportProvider.cameraStateFlow.debounce(0.5.seconds).collect {
            // since this LaunchedEffect is cancelled when not on the nearby transit
            // page, we don't need to check
            if (!nearbyTransit.viewportProvider.isFollowingPuck) {
                targetLocation = it.center.toPosition()
            }
        }
    }
    LaunchedEffect(nearbyTransit.viewportProvider.isManuallyCentering) {
        if (nearbyTransit.viewportProvider.isManuallyCentering) {
            nearbyViewModel.reset()
            targetLocation = null
        }
    }
    LaunchedEffect(nearbyTransit.viewportProvider.isFollowingPuck) {
        if (nearbyTransit.viewportProvider.isFollowingPuck) {
            nearbyViewModel.reset()
            targetLocation =
                nearbyTransit.locationDataManager.currentLocation.value?.let {
                    Position(it.longitude, it.latitude)
                }
        }
    }

    fun panToDefaultCenter() {
        nearbyTransit.viewportProvider.isManuallyCentering = true
        nearbyTransit.viewportProvider.isFollowingPuck = false
        nearbyTransit.viewportProvider.animateTo(
            ViewportProvider.Companion.Defaults.center,
            zoom = 13.75,
        )
    }

    NearbyTransitView(
        alertData = nearbyTransit.alertData,
        globalResponse = nearbyTransit.globalResponse,
        targetLocation = targetLocation,
        setLastLocation = { nearbyTransit.lastNearbyTransitLocation = it },
        setSelectingLocation = { nearbyTransit.nearbyTransitSelectingLocation = it },
        onOpenStopDetails = onOpenStopDetails,
        noNearbyStopsView = {
            NoNearbyStopsView(nearbyTransit.hideMaps, openSearch, ::panToDefaultCenter)
        },
        errorBannerViewModel = errorBannerViewModel,
    )
}
