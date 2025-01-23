package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import kotlinx.datetime.Instant

@Composable
fun TripDetailsView(
    tripFilter: TripDetailsFilter?,
    stopId: String,
    stopDetailsVM: StopDetailsViewModel,
    now: Instant
) {

    TripDetailsHeader()
    TripStops()
}
