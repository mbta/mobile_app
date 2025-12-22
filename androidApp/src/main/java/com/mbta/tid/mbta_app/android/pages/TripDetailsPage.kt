package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.DebugView
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ScrollSeparatorColumn
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.stopDetails.AlertCardSpec
import com.mbta.tid.mbta_app.android.stopDetails.TripDetailsView
import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.android.tripDetails.TripDetailsPageHeader
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.NavigationCallbacks
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import com.mbta.tid.mbta_app.viewModel.ITripDetailsPageViewModel
import com.mbta.tid.mbta_app.viewModel.ITripDetailsViewModel
import kotlin.time.Duration.Companion.seconds
import org.koin.compose.currentKoinScope
import org.koin.compose.koinInject

@Composable
fun TripDetailsPage(
    filter: TripDetailsPageFilter,
    allAlerts: AlertsStreamDataResponse?,
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    navCallbacks: NavigationCallbacks,
    errorBannerViewModel: IErrorBannerViewModel = koinInject(),
    tripDetailsPageVM: ITripDetailsPageViewModel = koinInject(),
    tripDetailsVM: ITripDetailsViewModel = koinInject(),
) {
    val now by timer(updateInterval = 5.seconds)
    val global = getGlobalData("TripDetailsPage")

    tripDetailsPageVM.koinScope = currentKoinScope()
    val tripDetailsPageState by tripDetailsPageVM.models.collectAsState()
    val tripDetailsState by tripDetailsVM.models.collectAsState()

    val route = global?.getRoute(tripDetailsPageState.trip?.routeId ?: filter.routeId as? Route.Id)
    val accents = route?.let { TripRouteAccents(it) } ?: TripRouteAccents.default

    LaunchedEffect(allAlerts) { tripDetailsPageVM.setAlerts(allAlerts) }
    LaunchedEffect(filter) { tripDetailsPageVM.setFilter(filter) }
    LaunchedEffect(tripDetailsState.awaitingPredictionsAfterBackground) {
        errorBannerViewModel.setIsLoadingWhenPredictionsStale(
            tripDetailsState.awaitingPredictionsAfterBackground
        )
    }

    val direction = tripDetailsPageState.direction
    val alertSummaries = tripDetailsPageState.alertSummaries

    fun openAlertDetails(alert: Alert, spec: AlertCardSpec) {
        openModal(
            ModalRoutes.AlertDetails(
                alertId = alert.id,
                lineId = null,
                routeIds = if (spec == AlertCardSpec.Elevator) null else listOfNotNull(route?.id),
                stopId = filter.stopId,
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (route != null && direction != null) {
            TripDetailsPageHeader(route, direction, navCallbacks)
        } else {
            CompositionLocalProvider(IsLoadingSheetContents provides true) {
                TripDetailsPageHeader(
                    route ?: ObjectCollectionBuilder.Single.route(),
                    Direction(
                        stringResource(R.string.loading),
                        stringResource(R.string.loading),
                        0,
                    ),
                    navCallbacks,
                )
            }
        }
        ErrorBanner(errorBannerViewModel, Modifier.padding(bottom = 8.dp))
        DebugView {
            val textColor = accents?.textColor ?: Color.Unspecified
            Column(
                Modifier.align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("trip id: ${filter.tripId}", color = textColor)
                Text("vehicle id: ${filter.vehicleId ?: "null"}", color = textColor)
            }
        }
        ScrollSeparatorColumn {
            TripDetailsView(
                filter,
                allAlerts = allAlerts,
                alertSummaries = alertSummaries,
                onOpenAlertDetails = { openAlertDetails(it, AlertCardSpec.Downstream) },
                openSheetRoute = openSheetRoute,
                openModal = openModal,
                now = now,
                routeAccents = accents,
                isTripDetailsPage = true,
                modifier = Modifier.padding(horizontal = 10.dp).navigationBarsPadding(),
            )
        }
    }
}
