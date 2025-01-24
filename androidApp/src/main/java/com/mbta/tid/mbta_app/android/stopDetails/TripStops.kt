package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripHeaderSpec
import kotlinx.datetime.Instant

@Composable
fun TripStops(
    targetId: String,
    stops: TripDetailsStopList,
    stopSequence: Int?,
    headerSpec: TripHeaderSpec?,
    now: Instant,
    global: GlobalResponse?
) {}
