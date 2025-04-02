package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.component.PinButton
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.datetime.Instant

@Composable
fun RouteCard(
    data: RouteCardData,
    globalData: GlobalResponse?,
    now: Instant,
    pinned: Boolean,
    onPin: (String) -> Unit,
    showElevatorAccessibility: Boolean = false,
    onOpenStopDetails: (String, StopDetailsFilter) -> Unit
) {
    Column(Modifier.haloContainer(1.dp)) {
        TransitHeader(data.lineOrRoute) { color ->
            PinButton(pinned = pinned, color = color) { onPin(data.lineOrRoute.id) }
        }

        data.stopData.forEach {
            if (data.context == RouteCardData.Context.NearbyTransit) {
                StopHeader(it, showElevatorAccessibility)
            }

            Departures(it, data, globalData, now, pinned) { leaf ->
                onOpenStopDetails(
                    it.stop.id,
                    StopDetailsFilter(data.lineOrRoute.id, leaf.directionId)
                )
            }
        }
    }
}
