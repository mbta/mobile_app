package com.mbta.tid.mbta_app.model.stopDetailsPage

import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.utils.EasternTimeInstant

public data class TileData(
    val route: Route?,
    val headsign: String?,
    val formatted: UpcomingFormat,
    val upcoming: UpcomingTrip,
) {
    val id: String = upcoming.id

    public fun isSelected(tripFilter: TripDetailsFilter?): Boolean =
        upcoming.trip.id == tripFilter?.tripId &&
            tripFilter.stopSequence?.let { filterSequence ->
                upcoming.stopSequence?.let { it == filterSequence }
            } ?: true

    internal companion object {
        fun fromUpcoming(upcoming: UpcomingTrip, route: Route, now: EasternTimeInstant): TileData? {
            val formattedUpcomingTrip =
                upcoming.format(
                    now,
                    route,
                    context = TripInstantDisplay.Context.StopDetailsFiltered,
                    lastTrip = false,
                )
            val formatted =
                if (formattedUpcomingTrip != null) {
                    UpcomingFormat.Some(
                        trips = listOf(formattedUpcomingTrip),
                        secondaryAlert = null,
                    )
                } else {
                    return null
                }

            return TileData(route, upcoming.trip.headsign, formatted, upcoming)
        }
    }
}
