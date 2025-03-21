package com.mbta.tid.mbta_app.android.stopDetails

import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.UpcomingTrip
import kotlinx.datetime.Instant

data class TileData(
    val id: String,
    val route: Route,
    val headsign: String,
    val formatted: UpcomingFormat,
    val upcoming: UpcomingTrip
) {
    companion object {
        fun fromUpcoming(upcoming: UpcomingTrip, route: Route, now: Instant): TileData? {
            val formattedUpcomingTrip =
                RealtimePatterns.formatUpcomingTrip(
                    now,
                    upcoming,
                    route.type,
                    context = TripInstantDisplay.Context.StopDetailsFiltered
                )
            val formatted =
                if (formattedUpcomingTrip != null) {
                    UpcomingFormat.Some(
                        trips = listOf(formattedUpcomingTrip),
                        secondaryAlert = null
                    )
                } else {
                    UpcomingFormat.NoTrips(
                        noTripsFormat = UpcomingFormat.NoTripsFormat.PredictionsUnavailable
                    )
                }

            if (formatted !is UpcomingFormat.Some) {
                return null
            }

            return TileData(upcoming.trip.id, route, upcoming.trip.headsign, formatted, upcoming)
        }
    }
}
