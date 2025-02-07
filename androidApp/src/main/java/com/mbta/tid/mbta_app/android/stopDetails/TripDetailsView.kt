package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.android.util.rememberSuspend
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.ExplainerType
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripData
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripHeaderSpec
import com.mbta.tid.mbta_app.utils.resolveParentId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import org.koin.compose.koinInject

@Composable
fun TripDetailsView(
    tripFilter: TripDetailsFilter?,
    stopId: String,
    stopDetailsVM: StopDetailsViewModel,
    setMapSelectedVehicle: (Vehicle?) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    openModal: (ModalRoutes) -> Unit,
    now: Instant,
    analytics: Analytics = koinInject(),
    modifier: Modifier = Modifier
) {

    val tripData: TripData? = stopDetailsVM.tripData.collectAsState().value
    val globalResponse: GlobalResponse? = getGlobalData(errorKey = "TripDetailsView.getGlobalData")
    val vehicle = tripData?.vehicle

    fun getParentFor(stopId: String?, stops: Map<String, Stop>): Stop? {
        return stopId.let { stops[stopId]?.resolveParent(stops) }
    }

    fun onTapStop(stop: TripDetailsStopList.Entry) {
        val parentStationId = globalResponse?.stops?.resolveParentId(stop.stop.id) ?: stop.stop.id
        openSheetRoute(SheetRoutes.StopDetails(parentStationId, null, null))
        analytics.tappedDownstreamStop(
            routeId = tripData?.trip?.routeId ?: "",
            stopId = parentStationId,
            tripId = tripFilter?.tripId ?: "",
            connectingRouteId = null
        )
    }

    LaunchedEffect(vehicle) {
        if (vehicle?.id == tripFilter?.vehicleId) {
            setMapSelectedVehicle(vehicle)
        } else {
            setMapSelectedVehicle(null)
        }
    }

    val stops =
        rememberSuspend(tripFilter, tripData, globalResponse) {
            withContext(Dispatchers.Default) {
                if (
                    tripFilter != null &&
                        tripData != null &&
                        tripData.tripFilter == tripFilter &&
                        tripData.tripPredictionsLoaded &&
                        globalResponse != null
                ) {
                    TripDetailsStopList.fromPieces(
                        tripFilter.tripId,
                        tripData.trip.directionId,
                        tripData.tripSchedules,
                        tripData.tripPredictions,
                        vehicle,
                        // TODO alerts
                        AlertsStreamDataResponse(mapOf()),
                        globalResponse
                    )
                } else {
                    null
                }
            }
        }

    if (tripFilter != null && tripData != null && globalResponse != null && stops != null) {
        val route = globalResponse.routes[tripData.trip.routeId]
        val routeAccents = route?.let { TripRouteAccents(it) } ?: TripRouteAccents.default
        val terminalStop = getParentFor(tripData.trip.stopIds?.firstOrNull(), globalResponse.stops)
        val vehicleStop =
            if (vehicle != null) getParentFor(vehicle.stopId, globalResponse.stops) else null
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
                    globalResponse ?: GlobalResponse(ObjectCollectionBuilder())
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
    modifier: Modifier = Modifier
) {
    Column(modifier) {
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
                routeAccents
            )
        }
    }
}
