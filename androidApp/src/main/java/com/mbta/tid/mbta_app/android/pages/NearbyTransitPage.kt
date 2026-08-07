package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.DebugView
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.component.routeCard.RouteCardList
import com.mbta.tid.mbta_app.android.nearbyTransit.NoNearbyStopsView
import com.mbta.tid.mbta_app.android.util.isRoughlyEqualTo
import com.mbta.tid.mbta_app.android.util.manageFavorites
import com.mbta.tid.mbta_app.android.util.managedTargetLocation
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.android.util.toPosition
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.utils.NavigationCallbacks
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import com.mbta.tid.mbta_app.viewModel.INearbyViewModel
import kotlin.time.Duration.Companion.seconds

@Composable
fun NearbyTransitPage(
    nearbyTransit: NearbyTransit,
    onOpenStopDetails: (String, StopDetailsFilter?) -> Unit,
    openSearch: () -> Unit,
    nearbyViewModel: INearbyViewModel,
    errorBannerViewModel: IErrorBannerViewModel,
) {

    val alertData = nearbyTransit.alertData
    val globalResponse = nearbyTransit.globalResponse

    val now by timer(updateInterval = 5.seconds)
    val state by nearbyViewModel.models.collectAsStateWithLifecycle()
    val (favorites) = manageFavorites()

    val targetLocation by managedTargetLocation(nearbyTransit)
    val cameraStateUnthrottled by
        nearbyTransit.viewportProvider.cameraStateFlow.collectAsStateWithLifecycle(null)

    LaunchedEffect(now) { nearbyViewModel.setNow(now) }
    LaunchedEffect(alertData) { nearbyViewModel.setAlerts(alertData) }
    LaunchedEffect(targetLocation, nearbyTransit.viewportProvider.isManuallyCentering) {
        if (!nearbyTransit.viewportProvider.isManuallyCentering) {
            nearbyViewModel.setLocation(targetLocation)
        }
    }

    LaunchedEffect(Unit) { nearbyViewModel.setActive(active = true, wasSentToBackground = false) }

    LifecycleResumeEffect(Unit) {
        nearbyViewModel.setActive(active = true, wasSentToBackground = false)
        onPauseOrDispose { nearbyViewModel.setActive(active = false, wasSentToBackground = true) }
    }

    LaunchedEffect(state.awaitingPredictionsAfterBackground) {
        errorBannerViewModel.setIsLoadingWhenPredictionsStale(
            state.awaitingPredictionsAfterBackground
        )
    }

    LaunchedEffect(targetLocation) {
        targetLocation?.let {
            cameraStateUnthrottled?.center?.toPosition()?.let { currentPosition ->
                if (
                    it.isRoughlyEqualTo(currentPosition) &&
                        !nearbyTransit.viewportProvider.isManuallyCentering
                ) {
                    nearbyTransit.lastLoadedLocation = it
                    nearbyTransit.isTargeting = false
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SheetHeader(
            title = stringResource(R.string.nearby_transit),
            navCallbacks =
                NavigationCallbacks(
                    onBack = null,
                    onClose = null,
                    backButtonPresentation = NavigationCallbacks.BackButtonPresentation.Floating,
                ),
        )
        ErrorBanner(errorBannerViewModel)
        DebugView(content = {})
        RouteCardList(
            routeCardData = state.routeCardData,
            emptyView = {
                NoNearbyStopsView(openSearch, nearbyTransit.viewportProvider::panToDefaultCenter)
                Spacer(Modifier.weight(1f))
            },
            global = globalResponse,
            now = now,
            isFavorite = { rsd -> (favorites?.keys ?: emptySet()).contains(rsd) },
            onOpenStopDetails = onOpenStopDetails,
        )
    }
}
