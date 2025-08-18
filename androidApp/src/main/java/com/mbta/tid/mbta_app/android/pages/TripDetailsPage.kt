package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter

@Composable
fun TripDetailsPage(filter: TripDetailsPageFilter, onClose: () -> Unit) {
    Column {
        SheetHeader(title = "", onClose = onClose)
        Text("TODO: Populate with actual trip view")
        Text("Trip: ${filter.tripId}")
        Text("Vehicle: ${filter.vehicleId}")
        Text("Route: ${filter.routeId}")
        Text("Direction: ${filter.directionId}")
        Text("Stop: ${filter.stopId}")
        Text("Stop Sequence: ${filter.stopSequence}")
    }
}
