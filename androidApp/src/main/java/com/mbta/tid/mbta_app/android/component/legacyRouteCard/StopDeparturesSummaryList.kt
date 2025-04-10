package com.mbta.tid.mbta_app.android.component.legacyRouteCard

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.DirectionRowView
import com.mbta.tid.mbta_app.android.component.HeadsignRowView
import com.mbta.tid.mbta_app.android.component.NavDrilldownRow
import com.mbta.tid.mbta_app.android.component.PillDecoration
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingFormat
import kotlinx.datetime.Instant
import org.koin.compose.koinInject

@Composable
fun StopDeparturesSummaryList(
    patternsAtStop: PatternsByStop,
    condenseHeadsignPredictions: Boolean,
    now: Instant,
    context: TripInstantDisplay.Context,
    pinned: Boolean,
    analytics: Analytics = koinInject(),
    onClick: (RealtimePatterns) -> Unit
) {
    val localContext = LocalContext.current

    for (patterns in patternsAtStop.patterns) {
        fun analyticsTappedDeparture(predictions: UpcomingFormat) {
            val noTrips =
                when (predictions) {
                    is UpcomingFormat.NoTrips -> predictions.noTripsFormat
                    else -> null
                }
            analytics.tappedDeparture(
                patternsAtStop.routeIdentifier,
                patternsAtStop.stop.id,
                pinned,
                !patterns.alertsHere.isNullOrEmpty(),
                patternsAtStop.representativeRoute.type,
                noTrips
            )
        }

        when (patterns) {
            is RealtimePatterns.ByHeadsign -> {
                val predictions =
                    patterns.format(
                        now,
                        patternsAtStop.representativeRoute.type,
                        if (condenseHeadsignPredictions) 1 else 2,
                        context
                    )
                NavDrilldownRow(
                    onClick = {
                        onClick(patterns)
                        analyticsTappedDeparture(predictions)
                    },
                    onClickLabel = localContext.getString(R.string.open_for_more_arrivals),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) { modifier ->
                    HeadsignRowView(
                        patterns.headsign,
                        predictions,
                        modifier = modifier,
                        pillDecoration =
                            if (patternsAtStop.line != null) PillDecoration.OnRow(patterns.route)
                            else null
                    )
                }
            }
            is RealtimePatterns.ByDirection -> {
                val predictions =
                    patterns.format(now, patternsAtStop.representativeRoute.type, context)
                NavDrilldownRow(
                    onClick = {
                        onClick(patterns)
                        analyticsTappedDeparture(predictions)
                    },
                    onClickLabel = localContext.getString(R.string.open_for_more_arrivals),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) { modifier ->
                    DirectionRowView(
                        patterns.direction,
                        predictions,
                        modifier = modifier,
                        pillDecoration = PillDecoration.OnPrediction(patterns.routesByTrip)
                    )
                }
            }
        }

        if (patterns != patternsAtStop.patterns.last()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surface)
        }
    }
}
