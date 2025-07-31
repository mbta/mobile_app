package com.mbta.tid.mbta_app.android.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitView
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitViewModel
import com.mbta.tid.mbta_app.android.nearbyTransit.NoNearbyStopsView
import com.mbta.tid.mbta_app.android.util.managedTargetLocation
import com.mbta.tid.mbta_app.model.StopDetailsFilter

@Composable
fun NearbyTransitPage(
    nearbyTransit: NearbyTransit,
    onOpenStopDetails: (String, StopDetailsFilter?) -> Unit,
    openSearch: () -> Unit,
    nearbyViewModel: NearbyTransitViewModel,
    errorBannerViewModel: ErrorBannerViewModel,
) {
    val targetLocation by managedTargetLocation(nearbyTransit) { nearbyViewModel.reset() }

    NearbyTransitView(
        alertData = nearbyTransit.alertData,
        globalResponse = nearbyTransit.globalResponse,
        targetLocation = targetLocation,
        setLastLocation = { nearbyTransit.lastLoadedLocation = it },
        setIsTargeting = { nearbyTransit.isTargeting = it },
        onOpenStopDetails = onOpenStopDetails,
        noNearbyStopsView = {
            NoNearbyStopsView(openSearch, nearbyTransit.viewportProvider::panToDefaultCenter)
        },
        errorBannerViewModel = errorBannerViewModel,
    )
}
