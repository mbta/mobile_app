package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.component.DebugView
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.ExplainerType
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripData
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripHeaderSpec
import kotlinx.datetime.Instant
import org.koin.compose.koinInject

@Composable
fun TripDetailsView(
    tripFilter: TripDetailsFilter?,
    stopId: String,
    allAlerts: AlertsStreamDataResponse?,
    stopDetailsVM: StopDetailsViewModel,
    openSheetRoute: (SheetRoutes) -> Unit,
    openModal: (ModalRoutes) -> Unit,
    now: Instant,
    analytics: Analytics = koinInject(),
    modifier: Modifier = Modifier
) {
    val showStationAccessibility = stopDetailsVM.showStationAccessibility.collectAsState().value
    val tripData: TripData? = stopDetailsVM.tripData.collectAsState().value
    val globalResponse: GlobalResponse? = getGlobalData(errorKey = "TripDetailsView.getGlobalData")
    val vehicle = tripData?.vehicle

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
            connectingRouteId = null
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

        TripDetailsView(
            tripId,
            headerSpec,
            onHeaderTap,
            ::onTapStop,
            routeAccents,
            stopId,
            stops,
            tripFilter,
            now,
            globalResponse,
            showStationAccessibility,
            modifier
        )
    } else {
        val placeholderTripInfo = LoadingPlaceholders.tripDetailsInfo()
        val placeholderTripStops = LoadingPlaceholders.tripDetailsStops()
        val placeholderTripId = placeholderTripInfo.vehicle.tripId ?: ""

        val placeholderHeaderSpec =
            TripHeaderSpec.getSpec(
                placeholderTripId,
                placeholderTripInfo.stops,
                null,
                placeholderTripInfo.vehicle,
                placeholderTripInfo.vehicleStop
            )
        val placeholderRouteAccents = TripRouteAccents(placeholderTripInfo.route)

        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            Column(modifier = modifier.loadingShimmer()) {
                TripDetailsView(
                    placeholderTripId,
                    placeholderHeaderSpec,
                    null,
                    onTapStop = {},
                    placeholderRouteAccents,
                    stopId,
                    placeholderTripStops,
                    tripFilter,
                    now,
                    globalResponse ?: GlobalResponse(ObjectCollectionBuilder()),
                    showStationAccessibility
                )
            }
        }
    }
}

@Composable
private fun TripDetailsView(
    tripId: String,
    headerSpec: TripHeaderSpec?,
    onHeaderTap: (() -> Unit)?,
    onTapStop: (TripDetailsStopList.Entry) -> Unit,
    routeAccents: TripRouteAccents,
    stopId: String,
    stops: TripDetailsStopList,
    tripFilter: TripDetailsFilter?,
    now: Instant,
    globalResponse: GlobalResponse,
    showStationAccessibility: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        DebugView {
            Column(
                Modifier.align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("trip id: ${tripFilter?.tripId ?: "null"}")
                Text("vehicle id: ${tripFilter?.vehicleId ?: "null"}")
            }
        }
        Column(Modifier.zIndex(1F)) {
            TripHeaderCard(tripId, headerSpec, stopId, routeAccents, now, onTap = onHeaderTap)
        }
        Column(Modifier.offset(y = (-16).dp).padding(horizontal = 4.dp)) {
            TripStops(
                stopId,
                stops,
                tripFilter?.stopSequence,
                headerSpec,
                now,
                globalResponse,
                onTapStop,
                routeAccents,
                showStationAccessibility
            )
        }
    }
}
