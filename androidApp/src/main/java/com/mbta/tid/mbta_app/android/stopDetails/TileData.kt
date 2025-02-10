package com.mbta.tid.mbta_app.android.stopDetails

import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingTrip
import kotlinx.datetime.Instant

data class TileData(
    val id: String,
    val route: Route,
    val headsign: String,
    val formatted: RealtimePatterns.Format,
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
                    RealtimePatterns.Format.Some(
                        trips = listOf(formattedUpcomingTrip),
                        secondaryAlert = null
                    )
                } else {
                    RealtimePatterns.Format.NoTrips(
                        noTripsFormat = RealtimePatterns.NoTripsFormat.PredictionsUnavailable
                    )
                }

            if (formatted !is RealtimePatterns.Format.Some) {
                return null
            }

            return TileData(upcoming.trip.id, route, upcoming.trip.headsign, formatted, upcoming)
        }
    }
}
