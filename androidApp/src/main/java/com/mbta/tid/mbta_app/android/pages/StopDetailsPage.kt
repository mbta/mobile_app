package com.mbta.tid.mbta_app.android.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.mbta.tid.mbta_app.android.util.StopDetailsFilter
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse

@Composable
fun StopDetailsPage(
    stop: Stop,
    filterState: MutableState<StopDetailsFilter?>,
    alertData: AlertsStreamDataResponse?,
    onClose: () -> Unit
) {}
