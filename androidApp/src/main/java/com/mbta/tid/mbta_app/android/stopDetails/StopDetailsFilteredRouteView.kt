package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.HeadsignRowView
import com.mbta.tid.mbta_app.android.component.LineHeader
import com.mbta.tid.mbta_app.android.component.PillDecoration
import com.mbta.tid.mbta_app.android.component.RouteHeader
import com.mbta.tid.mbta_app.android.util.StopDetailsFilter
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.datetime.Instant

@Composable
fun StopDetailsFilteredRouteView(
    departures: StopDetailsDepartures,
    global: GlobalResponse?,
    now: Instant,
    filterState: MutableState<StopDetailsFilter?>
) {
    val filter by filterState
    val patternsByStop = departures.routes.find { it.routeIdentifier == filter?.routeId }
    val expectedDirection = filter?.directionId
    if (patternsByStop == null) {
        return
    }
    val alerts: List<Alert> =
        if (expectedDirection != null && global != null) {
            patternsByStop.alertsHereFor(directionId = expectedDirection, global = global)
        } else {
            emptyList()
        }

    val rows: List<RowData> =
        patternsByStop.allUpcomingTrips().mapNotNull { upcoming ->
            val route = patternsByStop.routes.first { it.id == upcoming.trip.routeId }

            RowData.fromData(upcoming, route, expectedDirection, now)
        }

    val routeHex: String = patternsByStop.line?.color ?: patternsByStop.representativeRoute.color
    val routeColor: Color = Color.fromHex(routeHex)
    Column(
        Modifier.padding(start = 8.dp, top = 8.dp, bottom = 32.dp, end = 8.dp)
            .background(routeColor)
            .verticalScroll(rememberScrollState())
    ) {
        if (patternsByStop.line != null) {
            LineHeader(patternsByStop.line!!, patternsByStop.routes)
        } else {
            RouteHeader(patternsByStop.representativeRoute)
        }
        DirectionPicker(patternsByStop, filterState)

        Column(Modifier.background(colorResource(R.color.fill3), RoundedCornerShape(4.dp))) {
            for ((index, alert) in alerts.withIndex()) {
                Column {
                    StopDetailsAlertHeader(alert, routeColor)
                    if (index < alerts.size - 1 || rows.isNotEmpty()) {
                        HorizontalDivider(Modifier.background(colorResource(R.color.halo)))
                    }
                }
            }
            for ((index, row) in rows.withIndex()) {
                Column {
                    HeadsignRowView(
                        row.headsign,
                        row.formatted,
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        pillDecoration =
                            if (patternsByStop.line != null) PillDecoration.OnRow(route = row.route)
                            else null
                    )

                    if (index < rows.size - 1) {
                        HorizontalDivider(Modifier.background(colorResource(R.color.halo)))
                    }
                }
            }
        }
    }
}

private data class RowData(
    val tripId: String,
    val route: Route,
    val headsign: String,
    val formatted: RealtimePatterns.Format
) {
    companion object {
        fun fromData(
            upcoming: UpcomingTrip,
            route: Route,
            expectedDirection: Int?,
            now: Instant
        ): RowData? {
            val trip = upcoming.trip
            if (trip.directionId != expectedDirection) {
                return null
            }

            val headsign = trip.headsign
            val formatted =
                RealtimePatterns.ByHeadsign(
                        route,
                        headsign,
                        line = null,
                        patterns = emptyList(),
                        upcomingTrips = listOf(upcoming),
                        alertsHere = null
                    )
                    .format(now, route.type, TripInstantDisplay.Context.StopDetailsFiltered)

            if (formatted !is RealtimePatterns.Format.Some) return null

            return RowData(trip.id, route, headsign, formatted)
        }
    }
}
