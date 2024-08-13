package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.StopDeparturesSummaryList
import com.mbta.tid.mbta_app.android.util.StopDetailsFilter
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import kotlinx.datetime.Instant

@Composable
fun NearbyStopView(
    patternsAtStop: PatternsByStop,
    condenseHeadsignPredictions: Boolean = false,
    now: Instant,
    onOpenStopDetails: (String, StopDetailsFilter?) -> Unit,
) {
    Row(
        modifier =
            Modifier.clickable { onOpenStopDetails(patternsAtStop.stop.id, null) }
                .background(colorResource(id = R.color.fill2))
                .fillMaxWidth()
    ) {
        Text(
            text = patternsAtStop.stop.name,
            modifier = Modifier.padding(top = 11.dp, bottom = 11.dp, start = 16.dp, end = 8.dp),
            style = MaterialTheme.typography.headlineSmall
        )
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
