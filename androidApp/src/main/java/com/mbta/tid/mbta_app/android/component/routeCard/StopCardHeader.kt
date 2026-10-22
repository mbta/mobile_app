package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.android.component.StopAccessibilitySubheader
import com.mbta.tid.mbta_app.android.component.stopCard.StopHeader
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.response.GlobalResponse

@Composable
fun StopCardHeader(
    stop: Stop,
    elevatorAlerts: List<Alert>,
    globalData: GlobalResponse?,
) {
    StopHeader(
        stop = stop,
        subheader = {
            StopAccessibilitySubheader(
                stop = stop,
                elevatorAlerts = elevatorAlerts,
                globalData = globalData,
            )
        },
    )
}

@Composable
fun StopCardHeader(
    data: RouteCardData.RouteStopData,
    globalData: GlobalResponse?,
) {
    StopCardHeader(data.stop, data.elevatorAlerts, globalData)
}
