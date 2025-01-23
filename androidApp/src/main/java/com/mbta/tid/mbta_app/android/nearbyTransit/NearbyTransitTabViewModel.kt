package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NearbyTransitTabViewModel : ViewModel() {

    private val _stopDetailsDepartures = MutableStateFlow<StopDetailsDepartures?>(null)
    val stopDetailsDepartures: StateFlow<StopDetailsDepartures?> = _stopDetailsDepartures

    fun setStopDetailsDepartures(departures: StopDetailsDepartures?) {
        _stopDetailsDepartures.value = departures
    }

    fun setStopFilter(
        lastNavEntry: SheetRoutes?,
        stopId: String,
        stopFilter: StopDetailsFilter?,
        popLastNavEntry: () -> Unit,
        pushNavEntry: (SheetRoutes) -> Unit
    ) {

        if (
            lastNavEntry is SheetRoutes.StopDetails &&
                stopId == lastNavEntry.stopId &&
                stopFilter != lastNavEntry.stopFilter
        ) {
            // When the stop filter changes, we want a new entry to be added (i.e. no pop) only when
            // you're on the unfiltered (lastFilter == nil) page, but if there is already a filter,
            // the entry with the old filter should be popped and replaced with the new value.
            if (lastNavEntry.stopFilter != null) {
                popLastNavEntry()
            }
            if (shouldSkipStopFilterUpdate(lastNavEntry, lastNavEntry.stopId, stopFilter)) {
                return
            }
            pushNavEntry(SheetRoutes.StopDetails(lastNavEntry.stopId, stopFilter, null))
        } else {
            pushNavEntry(SheetRoutes.StopDetails(stopId, stopFilter, null))
        }
    }

    fun setTripFilter(
        lastNavEntry: SheetRoutes?,
        stopId: String,
        tripFilter: TripDetailsFilter?,
        popLastNavEntry: () -> Unit,
        pushNavEntry: (SheetRoutes) -> Unit
    ) {
        if (
            lastNavEntry is SheetRoutes.StopDetails &&
                lastNavEntry.stopId == stopId &&
                lastNavEntry.tripFilter != tripFilter
        ) {
            popLastNavEntry()
            pushNavEntry(SheetRoutes.StopDetails(stopId, lastNavEntry.stopFilter, tripFilter))
        }
    }

    private fun shouldSkipStopFilterUpdate(
        lastNavEntry: SheetRoutes?,
        newStopId: String,
        newFilter: StopDetailsFilter?
    ): Boolean {
        // If the new filter is nil and there is already a nil filter in the stack for the same stop
        // ID,
        // we don't want a duplicate unfiltered entry, so skip appending a new one

        return when (lastNavEntry) {
            is SheetRoutes.StopDetails -> {
                lastNavEntry.stopId == newStopId &&
                    lastNavEntry.stopFilter == null &&
                    newFilter == null
            }
            else -> false
        }
    }
}
