package com.mbta.tid.mbta_app.android.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitView
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitViewModel
import com.mbta.tid.mbta_app.android.nearbyTransit.NoNearbyStopsView
import com.mbta.tid.mbta_app.android.util.managedTargetLocation
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.FlowPreview

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
    managedTargetLocation(
        nearbyTransit = nearbyTransit,
        updateTargetLocation = { targetLocation = it },
        reset = { nearbyViewModel.reset() },
    )

    NearbyTransitView(
        alertData = nearbyTransit.alertData,
        globalResponse = nearbyTransit.globalResponse,
        targetLocation = targetLocation,
        setLastLocation = { nearbyTransit.lastNearbyTransitLocation = it },
        setSelectingLocation = { nearbyTransit.nearbyTransitSelectingLocation = it },
        onOpenStopDetails = onOpenStopDetails,
        noNearbyStopsView = {
            NoNearbyStopsView(openSearch, nearbyTransit.viewportProvider::panToDefaultCenter)
        },
        errorBannerViewModel = errorBannerViewModel,
    )
}
