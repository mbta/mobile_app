package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NearbyTransitTabViewModel : ViewModel() {

    private val _stopDetailsDepartures = MutableStateFlow<StopDetailsDepartures?>(null)
    val stopDetailsDepartures: StateFlow<StopDetailsDepartures?> = _stopDetailsDepartures

    private val _currentNavEntry = MutableStateFlow<SheetRoutes?>(null)
    val currentNavEntry: StateFlow<SheetRoutes?> = _currentNavEntry

    /**
     * Record the current sheetRoute. Calling this function does *not* affect the navigation stack.
     * It as a helper function to more conveniently access the SheetRoute directly rather than
     * reading off `NavBackStackEntry`, where it may not always be known what SheetRoute type to
     * resolve to.
     */

    // https://stackoverflow.com/a/78911122
    fun recordCurrentNavEntry(sheetRoute: SheetRoutes?) {
        _currentNavEntry.value = sheetRoute
    }

    fun setStopDetailsDepartures(departures: StopDetailsDepartures?) {
        _stopDetailsDepartures.value = departures
    }

    // TODO: Move these to separate Navigation utils helper

    fun setStopFilter(
        lastNavEntry: SheetRoutes?,
        stopFilter: StopDetailsFilter?,
        popLastNavEntry: () -> Unit,
        pushNavEntry: (SheetRoutes) -> Unit
    ) {

        if (lastNavEntry is SheetRoutes.StopDetails) {
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

    fun lastSheetRoute(navController: NavController): SheetRoutes? {
        return navController.currentBackStackEntry?.toRoute<SheetRoutes?>()
    }
}
