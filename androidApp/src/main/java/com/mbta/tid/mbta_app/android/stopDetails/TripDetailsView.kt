package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.rememberSuspend
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
    analytics: Analytics = koinInject()
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
                } else null
            }
        }

    if (tripFilter != null && tripData != null && globalResponse != null && stops != null) {
        val route = globalResponse.routes[tripData.trip.routeId]
        val routeAccents = route?.let { TripRouteAccents(it) } ?: TripRouteAccents.default
        val terminalStop = getParentFor(tripData.trip.stopIds?.first(), globalResponse.stops)
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

        Column {
            Column(Modifier.zIndex(1F)) {
                TripHeaderCard(tripId, headerSpec, stopId, routeAccents, now, onTap = onHeaderTap)
            }
            Column(Modifier.offset(y = (-6).dp).padding(horizontal = 4.dp)) {
                TripStops(
                    stopId,
                    stops,
                    tripFilter.stopSequence,
                    headerSpec,
                    now,
                    globalResponse,
                    ::onTapStop,
                    routeAccents
                )
            }
        }
    } else {
        // TODO: loading
        CircularProgressIndicator()
    }
}
