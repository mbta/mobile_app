package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.StopDeparturesSummaryList
import com.mbta.tid.mbta_app.android.util.Typography
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
    pinned: Boolean,
    onOpenStopDetails: (String, StopDetailsFilter?) -> Unit,
    showElevatorAccessibility: Boolean = false
) {
    val hasElevatorAlerts = patternsAtStop.elevatorAlerts.isNotEmpty()
    val isWheelchairAccessible = patternsAtStop.stop.isWheelchairAccessible
    val hasAccessibilityWarning = hasElevatorAlerts || !isWheelchairAccessible
    Row(
        modifier =
            Modifier.background(colorResource(id = R.color.fill2))
                .fillMaxWidth()
                .padding(top = 11.dp, bottom = 11.dp, start = 8.dp, end = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showElevatorAccessibility && hasElevatorAlerts) {
                Image(
                    painterResource(R.drawable.elevator_alert),
                    "",
                    modifier = Modifier.height(24.dp).placeholderIfLoading().clearAndSetSemantics {}
                )
            } else if (showElevatorAccessibility && isWheelchairAccessible) {
                Image(
                    painterResource(R.drawable.wheelchair_accessible),
                    "",
                    modifier = Modifier.height(24.dp).placeholderIfLoading().clearAndSetSemantics {}
                )
            }
            Text(
                text = patternsAtStop.stop.name,
                modifier =
                    Modifier.placeholderIfLoading()
                        .padding(
                            start =
                                if (showElevatorAccessibility && hasElevatorAlerts) 0.dp else 8.dp
                        )
                        .weight(.4f),
                style = Typography.callout
            )
            if (showElevatorAccessibility && hasAccessibilityWarning) {
                Text(
                    text =
                        if (hasElevatorAlerts)
                            pluralStringResource(
                                R.plurals.elevator_closure_count,
                                patternsAtStop.elevatorAlerts.size,
                                patternsAtStop.elevatorAlerts.size
                            )
                        else stringResource(R.string.not_accessible),
                    modifier = Modifier.placeholderIfLoading().alpha(0.6f).weight(.6f),
                    style = Typography.footnote
                )
            }
        }
    }

    StopDeparturesSummaryList(
        patternsAtStop,
        condenseHeadsignPredictions,
        now,
        TripInstantDisplay.Context.NearbyTransit,
        pinned,
    ) { patterns ->
        onOpenStopDetails(
            patternsAtStop.stop.id,
            StopDetailsFilter(patternsAtStop.routeIdentifier, patterns.directionId())
        )
    }
}
