package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.component.DebugView
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.ExplainerType
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripData
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripHeaderSpec
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.ITripDetailsViewModel
import com.mbta.tid.mbta_app.viewModel.TripDetailsViewModel
import io.sentry.kotlin.multiplatform.Sentry
import org.koin.compose.koinInject

@Composable
fun TripDetailsView(
    tripFilter: TripDetailsPageFilter?,
    allAlerts: AlertsStreamDataResponse?,
    alertSummaries: Map<String, AlertSummary?>,
    onOpenAlertDetails: (Alert) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    openModal: (ModalRoutes) -> Unit,
    now: EasternTimeInstant,
    isTripDetailsPage: Boolean,
    modifier: Modifier = Modifier,
    tripDetailsVM: ITripDetailsViewModel = koinInject(),
    analytics: Analytics = koinInject(),
) {
    val globalResponse: GlobalResponse? = getGlobalData(errorKey = "TripDetailsView")
    val state by tripDetailsVM.models.collectAsState()
    val tripData: TripData? = state.tripData
    val stopList = state.stopList
    val vehicle = tripData?.vehicle

    val tripDetailsContext: TripDetailsViewModel.Context =
        if (isTripDetailsPage) TripDetailsViewModel.Context.TripDetails
        else TripDetailsViewModel.Context.StopDetails

    LaunchedEffect(tripFilter) { tripDetailsVM.setFilters(tripFilter) }
    LaunchedEffect(allAlerts) { tripDetailsVM.setAlerts(allAlerts) }

    LaunchedEffect(Unit) {
        tripDetailsVM.setContext(tripDetailsContext)
        tripDetailsVM.setActive(active = true, wasSentToBackground = false)
    }

    LifecycleResumeEffect(Unit) {
        tripDetailsVM.setActive(active = true, wasSentToBackground = false)
        onPauseOrDispose { tripDetailsVM.setActive(active = false, wasSentToBackground = true) }
    }

    fun getParentFor(stopId: String?, globalResponse: GlobalResponse): Stop? {
        return stopId.let { globalResponse.getStop(stopId)?.resolveParent(globalResponse) }
    }

    fun onTapStop(stop: TripDetailsStopList.Entry) {
        val parentStationId =
            globalResponse?.getStop(stop.stop.id)?.resolveParent(globalResponse)?.id ?: stop.stop.id
        openSheetRoute(SheetRoutes.StopDetails(parentStationId, null, null))
        analytics.tappedDownstreamStop(
            routeId = tripData?.trip?.routeId ?: "",
            stopId = parentStationId,
            tripId = tripFilter?.tripId ?: "",
            connectingRouteId = null,
        )
    }

    if (
        tripFilter != null &&
            tripData != null &&
            globalResponse != null &&
            stopList != null &&
            tripData.tripFilter == tripFilter &&
            stopList.trip.id == tripData.trip.id
    ) {
        val route =
            globalResponse.getRoute(tripData.trip.routeId)
                ?: run {
                    Sentry.captureMessage(
                        "Trip ${tripData.trip.id} on unknown route ${tripData.trip.routeId}"
                    )
                    return
                }
        val terminalStop = getParentFor(tripData.trip.stopIds?.firstOrNull(), globalResponse)
        val vehicleStop =
            if (vehicle != null) getParentFor(vehicle.stopId, globalResponse) else null
        val tripId = tripFilter.tripId
        val headerSpec: TripHeaderSpec? =
            TripHeaderSpec.getSpec(tripId, stopList, terminalStop, vehicle, vehicleStop)

        val explainerType: ExplainerType? =
            when (headerSpec) {
                is TripHeaderSpec.Scheduled ->
                    if (route.type != RouteType.FERRY) {
                        ExplainerType.NoPrediction
                    } else {
                        null
                    }
                is TripHeaderSpec.FinishingAnotherTrip -> ExplainerType.FinishingAnotherTrip
                is TripHeaderSpec.NoVehicle -> ExplainerType.NoVehicle
                else -> null
            }
        val onHeaderTap: (() -> Unit)? =
            if (explainerType != null) {
                { openModal(ModalRoutes.Explainer(explainerType, TripRouteAccents(route))) }
            } else {
                null
            }

        val onFollowTrip: (() -> Unit) = {
            openSheetRoute(
                SheetRoutes.TripDetails(
                    TripDetailsPageFilter(
                        tripId,
                        tripData.tripFilter.vehicleId,
                        tripData.trip.routeId,
                        tripData.trip.directionId,
                        tripFilter.stopId,
                        tripData.tripFilter.stopSequence,
                    )
                )
            )
        }

        TripDetails(
            tripData.trip,
            headerSpec,
            onHeaderTap,
            ::onTapStop,
            onFollowTrip,
            onOpenAlertDetails,
            route,
            tripFilter,
            stopList,
            now,
            alertSummaries,
            globalResponse,
            isTripDetailsPage,
            modifier,
        )
    } else {
        val placeholderTripInfo = LoadingPlaceholders.tripDetailsInfo()
        val placeholderTripStops = placeholderTripInfo.stops
        val placeholderTripId = placeholderTripInfo.trip.id

        val placeholderHeaderSpec =
            TripHeaderSpec.getSpec(
                placeholderTripId,
                placeholderTripInfo.stops,
                null,
                placeholderTripInfo.vehicle,
                placeholderTripInfo.vehicleStop,
            )

        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            Column(modifier = modifier.loadingShimmer()) {
                TripDetails(
                    placeholderTripInfo.trip,
                    placeholderHeaderSpec,
                    null,
                    onTapStop = {},
                    onFollowTrip = {},
                    onOpenAlertDetails = {},
                    placeholderTripInfo.route,
                    TripDetailsPageFilter("", "", "", 0, "", null),
                    placeholderTripStops,
                    now,
                    emptyMap(),
                    globalResponse ?: GlobalResponse(ObjectCollectionBuilder()),
                    isTripDetailsPage,
                )
            }
        }
    }
}

@Composable
fun TripDetails(
    trip: Trip,
    headerSpec: TripHeaderSpec?,
    onHeaderTap: (() -> Unit)?,
    onTapStop: (TripDetailsStopList.Entry) -> Unit,
    onFollowTrip: (() -> Unit),
    onOpenAlertDetails: (Alert) -> Unit,
    route: Route,
    tripFilter: TripDetailsPageFilter,
    stopList: TripDetailsStopList,
    now: EasternTimeInstant,
    alertSummaries: Map<String, AlertSummary?>,
    globalResponse: GlobalResponse,
    isTripDetailsPage: Boolean,
    modifier: Modifier = Modifier,
) {
    val routeAccents = TripRouteAccents(route)
    val hasTrackThisTrip = SettingsCache.get(Settings.TrackThisTrip)

    Column(modifier) {
        DebugView {
            Column(
                Modifier.align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("trip id: ${tripFilter.tripId}")
                Text("vehicle id: ${tripFilter.vehicleId ?: "null"}")
            }
        }
        Column(Modifier.zIndex(1F)) {
            TripHeaderCard(
                trip,
                headerSpec,
                tripFilter.stopId,
                route,
                routeAccents,
                now,
                onTap = onHeaderTap,
                onFollowTrip =
                    if (hasTrackThisTrip && !isTripDetailsPage) {
                        onFollowTrip
                    } else null,
            )
        }
        Column(Modifier.offset(y = (-16).dp)) {
            TripStops(
                tripFilter.stopId,
                stopList,
                tripFilter.stopSequence,
                headerSpec,
                now,
                alertSummaries,
                globalResponse,
                onTapStop,
                onOpenAlertDetails,
                route,
                routeAccents,
            )
        }
    }
}
