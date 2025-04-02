package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.DirectionLabel
import com.mbta.tid.mbta_app.android.component.DirectionRowView
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.HeadsignRowView
import com.mbta.tid.mbta_app.android.component.PillDecoration
import com.mbta.tid.mbta_app.model.LeafFormat
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.datetime.Instant
import org.koin.compose.koinInject

@Composable
fun Departures(
    stopData: RouteCardData.RouteStopData,
    cardData: RouteCardData,
    globalData: GlobalResponse?,
    now: Instant,
    pinned: Boolean,
    analytics: Analytics = koinInject(),
    onClick: (RouteCardData.Leaf) -> Unit,
) {
    val localContext = LocalContext.current

    stopData.data.withIndex().forEach { (index, leaf) ->
        fun analyticsTappedDeparture(leafFormat: LeafFormat) {
            val format = (leafFormat as? LeafFormat.Single)?.format
            val noTrips = (format as? UpcomingFormat.NoTrips)?.noTripsFormat
            analytics.tappedDeparture(
                cardData.lineOrRoute.id,
                stopData.stop.id,
                pinned,
                leaf.alertsHere.isNotEmpty(),
                cardData.lineOrRoute.type,
                noTrips
            )
        }

        val formatted =
            leaf.format(now, cardData.lineOrRoute.sortRoute, globalData, cardData.context)
        val direction = stopData.directions.first { it.id == leaf.directionId }

        val clickModifier =
            Modifier.clickable(
                onClickLabel = localContext.getString(R.string.open_for_more_arrivals),
                onClick = {
                    onClick(leaf)
                    analyticsTappedDeparture(formatted)
                }
            )

        Column(
            modifier = clickModifier,
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            when (formatted) {
                is LeafFormat.Single -> {
                    DirectionRowView(
                        direction.copy(destination = formatted.headsign ?: direction.destination),
                        formatted.format
                    )
                }
                is LeafFormat.Branched -> {
                    DirectionLabel(
                        direction,
                        Modifier.padding(start = 8.dp, top = 8.dp),
                        showDestination = false
                    )
                    for (branch in formatted.branches) {
                        HeadsignRowView(
                            branch.headsign,
                            branch.format,
                            pillDecoration = branch.route?.let { PillDecoration.OnRow(it) }
                        )
                    }
                }
            }
            if (index < stopData.data.lastIndex) {
                HaloSeparator()
            }
        }
    }
}
