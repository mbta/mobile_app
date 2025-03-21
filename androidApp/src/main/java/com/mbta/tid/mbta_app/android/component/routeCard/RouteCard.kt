package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.component.routeIcon
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import kotlinx.datetime.Instant

@Composable
fun RouteCard(
    data: RouteCardData,
    now: Instant,
    showElevatorAccessibility: Boolean = false,
    onOpenStopDetails: (String, StopDetailsFilter) -> Unit
) {
    val transit = data.lineOrRoute

    Column(Modifier.haloContainer(1.dp)) {
        val (modeIcon, modeDescription) = routeIcon(transit.sortRoute)
        TransitHeader(
            transit.name,
            routeType = transit.type,
            backgroundColor = Color.fromHex(transit.backgroundColor),
            textColor = Color.fromHex(transit.textColor),
            modeIcon = modeIcon,
            modeDescription = modeDescription,
            null
        )

        data.stopData.forEach {
            if (data.context == RouteCardData.Context.NearbyTransit) {
                StopHeader(it, showElevatorAccessibility)
            }

            Departures(it, data, now) { leaf ->
                onOpenStopDetails(
                    it.stop.id,
                    StopDetailsFilter(data.lineOrRoute.id, leaf.directionId)
                )
            }
        }
    }
}
