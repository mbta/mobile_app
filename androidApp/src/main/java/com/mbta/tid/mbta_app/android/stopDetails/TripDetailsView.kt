package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.ExplainerType
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripData
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripHeaderSpec
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import org.koin.compose.koinInject

@Composable
fun TripDetailsView(
    tripFilter: TripDetailsFilter?,
    stopId: String,
    allAlerts: AlertsStreamDataResponse?,
    alertSummaries: Map<String, AlertSummary?>,
    stopDetailsVM: StopDetailsViewModel,
    onOpenAlertDetails: (Alert) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    openModal: (ModalRoutes) -> Unit,
    now: EasternTimeInstant,
    analytics: Analytics = koinInject(),
    modifier: Modifier = Modifier,
) {
    val tripData: TripData? = stopDetailsVM.tripData.collectAsState().value
    val globalResponse: GlobalResponse? = getGlobalData(errorKey = "TripDetailsView.getGlobalData")
    val vehicle = tripData?.vehicle

    val hasTrackThisTrip = SettingsCache.get(Settings.TrackThisTrip)

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

    val stops =
        stopDetailsVM
            .getTripDetailsStopList(tripFilter, allAlerts, globalResponse)
            .collectAsState()
            .value

    if (tripFilter != null && tripData != null && globalResponse != null && stops != null) {
        val route = globalResponse.getRoute(tripData.trip.routeId)
        val routeAccents = route?.let { TripRouteAccents(it) } ?: TripRouteAccents.default
        val terminalStop = getParentFor(tripData.trip.stopIds?.firstOrNull(), globalResponse)
        val vehicleStop =
            if (vehicle != null) getParentFor(vehicle.stopId, globalResponse) else null
        val tripId = tripFilter.tripId
        val headerSpec: TripHeaderSpec? =
            TripHeaderSpec.getSpec(tripId, stops, terminalStop, vehicle, vehicleStop)

        val explainerType: ExplainerType? =
            when (headerSpec) {
                is TripHeaderSpec.Scheduled ->
                    if (routeAccents.type != RouteType.FERRY) {
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
                { openModal(ModalRoutes.Explainer(explainerType, routeAccents)) }
            } else {
                null
            }

        val onFollowTrip: (() -> Unit)? =
            if (hasTrackThisTrip) {
                {
                    openSheetRoute(
                        SheetRoutes.TripDetails(
                            TripDetailsPageFilter(
                                tripId,
                                tripData.tripFilter.vehicleId,
                                tripData.trip.routeId,
                                tripData.trip.directionId,
                                stopId,
                                tripData.tripFilter.stopSequence,
                            )
                        )
                    )
                }
            } else {
                null
            }

        TripDetailsView(
            tripData.trip,
            headerSpec,
            onHeaderTap,
            ::onTapStop,
            onFollowTrip,
            onOpenAlertDetails,
            routeAccents,
            stopId,
            stops,
            tripFilter,
            now,
            alertSummaries,
            globalResponse,
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
        val placeholderRouteAccents = TripRouteAccents(placeholderTripInfo.route)

        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            Column(modifier = modifier.loadingShimmer()) {
                TripDetailsView(
                    placeholderTripInfo.trip,
                    placeholderHeaderSpec,
                    null,
                    onTapStop = {},
                    onFollowTrip = null,
                    onOpenAlertDetails = {},
                    placeholderRouteAccents,
                    stopId,
                    placeholderTripStops,
                    tripFilter,
                    now,
                    emptyMap(),
                    globalResponse ?: GlobalResponse(ObjectCollectionBuilder()),
                )
            }
        }
    }
}

@Composable
private fun TripDetailsView(
    trip: Trip,
    headerSpec: TripHeaderSpec?,
    onHeaderTap: (() -> Unit)?,
    onTapStop: (TripDetailsStopList.Entry) -> Unit,
    onFollowTrip: (() -> Unit)?,
    onOpenAlertDetails: (Alert) -> Unit,
    routeAccents: TripRouteAccents,
    stopId: String,
    stops: TripDetailsStopList,
    tripFilter: TripDetailsFilter?,
    now: EasternTimeInstant,
    alertSummaries: Map<String, AlertSummary?>,
    globalResponse: GlobalResponse,
    modifier: Modifier = Modifier,
) {

    Column(modifier) {
        DebugView {
            Column(
                Modifier.align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("trip id: ${tripFilter?.tripId ?: "null"}")
                Text("vehicle id: ${tripFilter?.vehicleId ?: "null"}")
            }
        }
        Column(Modifier.zIndex(1F)) {
            TripHeaderCard(
                trip,
                headerSpec,
                stopId,
                routeAccents,
                now,
                onTap = onHeaderTap,
                onFollowTrip = onFollowTrip,
            )
        }
        Column(Modifier.offset(y = (-16).dp)) {
            TripStops(
                stopId,
                stops,
                tripFilter?.stopSequence,
                headerSpec,
                now,
                alertSummaries,
                globalResponse,
                onTapStop,
                onOpenAlertDetails,
                routeAccents,
            )
        }
    }
}
