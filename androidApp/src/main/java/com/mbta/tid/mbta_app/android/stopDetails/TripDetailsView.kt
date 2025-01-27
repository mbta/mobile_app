package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripData
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripHeaderSpec
import kotlinx.datetime.Instant

@Composable
fun TripDetailsView(
    tripFilter: TripDetailsFilter?,
    stopId: String,
    stopDetailsVM: StopDetailsViewModel,
    now: Instant
) {

    val tripData: TripData? = stopDetailsVM.tripData.collectAsState().value
    val globalResponse: GlobalResponse? = getGlobalData(errorKey = "TripDetailsView.getGlobalData")
    val vehicle = tripData?.vehicle

    fun getParentFor(stopId: String?, stops: Map<String, Stop>): Stop? {
        return stopId.let { stops[stopId]?.resolveParent(stops) }
    }

    if (
        tripFilter != null &&
            vehicle != null &&
            tripData.tripFilter == tripFilter &&
            tripData.tripPredictionsLoaded &&
            globalResponse != null
    ) {
        val stops =
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
        if (stops != null) {
            val route = globalResponse.routes[tripData.trip.routeId]
            val routeAccents = route?.let { TripRouteAccents(it) } ?: TripRouteAccents.default
            val terminalStop = getParentFor(tripData.trip.stopIds?.first(), globalResponse.stops)
            val vehicleStop = getParentFor(vehicle.stopId, globalResponse.stops)
            val tripId = tripFilter.tripId
            val headerSpec: TripHeaderSpec? =
                TripHeaderSpec.getSpec(tripId, stops, terminalStop, vehicle, vehicleStop)

            TripDetailsHeader(tripId, headerSpec)
            TripStops(
                stopId,
                stops,
                tripFilter.stopSequence,
                headerSpec,
                now,
                globalResponse,
                routeAccents
            )
        } else {
            // TODO: loading
            CircularProgressIndicator()
        }
    } else {
        // TODO: loading
        CircularProgressIndicator()
    }
}
