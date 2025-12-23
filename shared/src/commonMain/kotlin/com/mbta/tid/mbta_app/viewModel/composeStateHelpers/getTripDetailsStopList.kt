package com.mbta.tid.mbta_app.viewModel.composeStateHelpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripData

@Composable
public fun getTripDetailsStopList(
    tripFilter: TripDetailsPageFilter?,
    tripData: TripData?,
    allAlerts: AlertsStreamDataResponse?,
    globalResponse: GlobalResponse?,
): TripDetailsStopList? {
    var tripDetailsStopList: TripDetailsStopList? by remember { mutableStateOf(null) }

    LaunchedEffect(tripFilter, tripData, allAlerts, globalResponse) {
        tripDetailsStopList =
            if (
                tripFilter != null &&
                    tripData != null &&
                    tripData.trip != null &&
                    tripData.tripFilter == tripFilter &&
                    tripData.tripPredictionsLoaded &&
                    globalResponse != null
            ) {
                TripDetailsStopList.fromPieces(
                    tripData.trip,
                    tripData.tripSchedules,
                    tripData.tripPredictions,
                    tripData.vehicle,
                    allAlerts,
                    globalResponse,
                )
            } else {
                null
            }
    }

    return tripDetailsStopList
}
