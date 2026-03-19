package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.Alert.Effect
import com.mbta.tid.mbta_app.utils.EasternTimeInstant

/** highPriority: Alerts here & now lowPriority: Alerts later & downstream */
public data class DisplayAlerts(
    val highPriority: List<DisplayAlert>,
    val lowPriority: List<DisplayAlert>,
) {

    public val allAlerts: List<Alert> = (highPriority + lowPriority).map { it.alert }

    public companion object {
        /**
         * Alerts sorted into display order based on the following features.
         * - Time (now before later)
         * - Place (here before downstream)
         * - Elevator impact (optional, when includeElevatorAlerts = true)
         * - Significance (major before minor)
         * - Time (as a tiebreaker for later alerts - show earliest later first)
         *
         * The sorted result is split into two lists
         * - Here & Now
         * - Later & Downstream
         */
        public fun forAlertsAtStop(
            alertsHere: List<Alert>,
            alertsDownstream: List<Alert>,
            includeElevatorAlerts: Boolean = false,
            now: EasternTimeInstant,
        ): DisplayAlerts {
            val idsHere = alertsHere.map { it.id }.toSet()
            val allAlerts = alertsHere + alertsDownstream
            val (tier1, tier2) =
                allAlerts
                    .filter {
                        if (includeElevatorAlerts) {
                            true
                        } else {
                            it.effect != Effect.ElevatorClosure
                        }
                    }
                    .sortedWith(
                        compareByDescending<Alert> { it.isActive(now) }
                            .thenByDescending { idsHere.contains(it.id) }
                            .thenByDescending { it.significance(null) }
                            .thenBy { it.currentOrNextPeriod(now)?.start?.instant }
                    )
                    .map { DisplayAlert(it, !idsHere.contains(it.id)) }
                    .partition { it.alert.isActive(now) && !it.isDownstream }

            return DisplayAlerts(tier1, tier2)
        }
    }
}
