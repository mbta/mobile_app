package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ScrollSeparatorColumn
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.stopDetails.AlertCardSpec
import com.mbta.tid.mbta_app.android.stopDetails.TripDetailsView
import com.mbta.tid.mbta_app.android.tripDetails.TripDetailsPageHeader
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.viewModel.ITripDetailsPageViewModel
import kotlin.time.Duration.Companion.seconds
import org.koin.compose.currentKoinScope
import org.koin.compose.koinInject

@Composable
fun TripDetailsPage(
    filter: TripDetailsPageFilter,
    allAlerts: AlertsStreamDataResponse?,
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    onClose: () -> Unit,
    tripDetailsPageVM: ITripDetailsPageViewModel = koinInject(),
) {
    val now by timer(updateInterval = 5.seconds)
    val global = getGlobalData("TripDetailsPage")

    val route = global?.getRoute(filter.routeId)

    tripDetailsPageVM.koinScope = currentKoinScope()
    val tripDetailsPageState by tripDetailsPageVM.models.collectAsState()

    LaunchedEffect(allAlerts) { tripDetailsPageVM.setAlerts(allAlerts) }
    LaunchedEffect(filter) { tripDetailsPageVM.setFilter(filter) }

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
            TripDetailsPageHeader(route, direction, onClose)
        } else {
            CompositionLocalProvider(IsLoadingSheetContents provides true) {
                TripDetailsPageHeader(
                    route ?: ObjectCollectionBuilder.Single.route(),
                    Direction(
                        stringResource(R.string.loading),
                        stringResource(R.string.loading),
                        0,
                    ),
                    onClose,
                )
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
                isTripDetailsPage = true,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
    }
}
