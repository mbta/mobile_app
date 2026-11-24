package com.mbta.tid.mbta_app.android.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.stopOverview.StopOverviewView
import com.mbta.tid.mbta_app.android.util.manageFavorites
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteStopFilter
import com.mbta.tid.mbta_app.model.StopOverviewPageFilters
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.utils.NavigationCallbacks
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import com.mbta.tid.mbta_app.viewModel.IStopCentricViewModel
import kotlin.time.Duration.Companion.seconds
import org.koin.compose.koinInject

@Composable
fun StopOverviewPage(
    modifier: Modifier = Modifier,
    filters: StopOverviewPageFilters,
    allAlerts: AlertsStreamDataResponse?,
    navCallbacks: NavigationCallbacks,
    updateRouteStopFilter: (RouteStopFilter?) -> Unit,
    openModal: (ModalRoutes) -> Unit,
    errorBannerViewModel: IErrorBannerViewModel,
    stopCentricViewModel: IStopCentricViewModel = koinInject(),
) {
    val now by timer(updateInterval = 5.seconds)

    val stopId = filters.stopId

    val (favorites, _) = manageFavorites()

    val awaitingPredictionsAfterBackground =
        stopCentricViewModel.models.collectAsState().value.awaitingPredictionsAfterBackground

    fun isFavorite(routeStopDirection: RouteStopDirection): Boolean? {
        return favorites?.contains(routeStopDirection)
    }

    LaunchedEffect(allAlerts) { stopCentricViewModel.setAlerts(allAlerts) }
    LaunchedEffect(filters) { stopCentricViewModel.setFilters(filters) }
    LaunchedEffect(now) { stopCentricViewModel.setNow(now) }

    LaunchedEffect(Unit) {
        stopCentricViewModel.setActive(active = true, wasSentToBackground = false)
    }

    LifecycleResumeEffect(Unit) {
        stopCentricViewModel.setActive(active = true, wasSentToBackground = false)
        onPauseOrDispose {
            stopCentricViewModel.setActive(active = false, wasSentToBackground = true)
        }
    }

    LaunchedEffect(awaitingPredictionsAfterBackground) {
        errorBannerViewModel.setIsLoadingWhenPredictionsStale(awaitingPredictionsAfterBackground)
    }

    StopOverviewView(
        modifier,
        stopId,
        now,
        ::isFavorite,
        navCallbacks,
        updateRouteStopFilter,
        openModal,
        errorBannerViewModel,
        stopCentricViewModel,
    )
}
