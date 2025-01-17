package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.StopDeparturesSummaryList
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import kotlinx.datetime.Instant

@Composable
fun NearbyStopView(
    patternsAtStop: PatternsByStop,
    condenseHeadsignPredictions: Boolean = false,
    now: Instant,
    onOpenStopDetails: (String, StopDetailsFilter?) -> Unit,
    showElevatorAccessibility: Boolean = false
) {
    val hasElevatorAlerts = patternsAtStop.elevatorAlerts.isNotEmpty()
    Row(
        modifier =
            Modifier.background(colorResource(id = R.color.fill2))
                .fillMaxWidth()
                .padding(top = 11.dp, bottom = 11.dp, start = 16.dp, end = 8.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showElevatorAccessibility && hasElevatorAlerts) {
                    Image(
                        painterResource(R.drawable.accessibility_icon_inaccessible),
                        "",
                        modifier =
                            Modifier.size(24.dp).placeholderIfLoading().clearAndSetSemantics {}
                    )
                }
                Text(
                    text = patternsAtStop.stop.name,
                    modifier = Modifier.placeholderIfLoading(),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            if (showElevatorAccessibility && hasElevatorAlerts) {
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.elevator_closure_count,
                            patternsAtStop.elevatorAlerts.size,
                            patternsAtStop.elevatorAlerts.size
                        ),
                    modifier = Modifier.placeholderIfLoading().padding(top = 8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    StopDeparturesSummaryList(
        patternsAtStop,
        condenseHeadsignPredictions,
        now,
        TripInstantDisplay.Context.NearbyTransit
    ) { patterns ->
        onOpenStopDetails(
            patternsAtStop.stop.id,
            StopDetailsFilter(patternsAtStop.routeIdentifier, patterns.directionId())
        )
    }
}
