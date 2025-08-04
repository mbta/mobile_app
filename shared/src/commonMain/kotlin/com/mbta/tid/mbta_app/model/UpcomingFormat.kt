package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.utils.EasternTimeInstant

sealed class UpcomingFormat {
    sealed class NoTripsFormat {
        data object NoSchedulesToday : NoTripsFormat()

        data object ServiceEndedToday : NoTripsFormat()

        data object PredictionsUnavailable : NoTripsFormat()

        companion object {
            /**
             * Determine the appropriate NoTripsFormat to show. Only for use in situations where it
             * is already determined that there are no trips to show and we only need to determine
             * why.
             */
            fun fromUpcomingTrips(
                upcomingTrips: List<UpcomingTrip>,
                hasSchedulesToday: Boolean,
                now: EasternTimeInstant,
            ): NoTripsFormat {
                val hasUpcomingTrips =
                    upcomingTrips.any { it.time != null && it.time > now && !it.isCancelled }

                return when {
                    !hasSchedulesToday -> NoSchedulesToday
                    hasUpcomingTrips ->
                        // there are trips in the future but we're not showing them (maybe because
                        // we're
                        // on the subway and we have schedules but can't get predictions)
                        PredictionsUnavailable
                    else ->
                        // if there were schedules but there are no trips in the future, service is
                        // over
                        ServiceEndedToday
                }
            }
        }
    }

    abstract val secondaryAlert: SecondaryAlert?

    data class SecondaryAlert(val iconName: String) {
        constructor(
            alert: Alert,
            mapStopRoute: MapStopRoute?,
        ) : this(alert.alertState, mapStopRoute)

        constructor(
            alertState: StopAlertState,
            mapStopRoute: MapStopRoute?,
        ) : this(iconName(alertState, mapStopRoute))
    }

    data object Loading : UpcomingFormat() {
        override val secondaryAlert = null
    }

    data class Some(val trips: List<FormattedTrip>, override val secondaryAlert: SecondaryAlert?) :
        UpcomingFormat() {
        data class FormattedTrip(
            val trip: UpcomingTrip,
            val routeType: RouteType,
            val format: TripInstantDisplay,
        ) {
            val id: String
                get() = trip.id

            constructor(
                trip: UpcomingTrip,
                routeType: RouteType,
                now: EasternTimeInstant,
                context: TripInstantDisplay.Context,
            ) : this(trip, routeType, trip.display(now, routeType, context))

            override fun toString() = format.toString()
        }

        constructor(
            trip: FormattedTrip,
            secondaryAlert: SecondaryAlert?,
        ) : this(listOf(trip), secondaryAlert)
    }

    data class NoTrips
    @DefaultArgumentInterop.Enabled
    constructor(
        val noTripsFormat: NoTripsFormat,
        override val secondaryAlert: SecondaryAlert? = null,
    ) : UpcomingFormat()

    data class Disruption(val alert: Alert, val iconName: String) : UpcomingFormat() {
        override val secondaryAlert = null

        constructor(
            alert: Alert,
            mapStopRoute: MapStopRoute?,
        ) : this(alert, iconName(alert.alertState, mapStopRoute))
    }

    companion object {
        private fun iconName(alertState: StopAlertState, mapStopRoute: MapStopRoute?) =
            "alert-${mapStopRoute?.let { "large-${it.name.lowercase()}" } ?: "borderless"}-${alertState.name.lowercase()}"
    }
}
