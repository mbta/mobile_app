package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.android.component.UpcomingTripViewState

@Composable
/** Helper function for formatting UpcomingTripView screen reader text for TalkBack. */
fun UpcomingTripViewState.contentDescription(isFirst: Boolean, vehicleType: String): String =
    if (this !is UpcomingTripViewState.Some) ""
    else this.trip.contentDescription(isFirst, vehicleType)
