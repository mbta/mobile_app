package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.model.RouteStopFilter
import com.mbta.tid.mbta_app.model.RouteStopFilter.Companion.shouldPopLastStopEntry
import com.mbta.tid.mbta_app.model.RouteStopPageFilters
import com.mbta.tid.mbta_app.model.StopOverviewPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.routes.SheetRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NearbyTransitTabViewModel : ViewModel() {

    private val _searchBarHeight = MutableStateFlow<Dp?>(null)
    val searchBarHeight: StateFlow<Dp?> = _searchBarHeight

    fun setSearchBarHeight(height: Dp) {
        _searchBarHeight.value = height
    }

    fun setStopOverviewFilters(
        lastNavEntry: SheetRoutes?,
        filters: StopOverviewPageFilters,
        pushNavEntry: (SheetRoutes) -> Unit,
    ) {
        if (lastNavEntry is SheetRoutes.StopOverview && filters.stopId == lastNavEntry.stopId) {
            return
        }

        pushNavEntry(
            SheetRoutes.StopOverview(filters.stopId),
        )
    }

    fun setRouteStopFilters(
        lastNavEntry: SheetRoutes?,
        filters: RouteStopPageFilters,
        popLastNavEntry: () -> Unit,
        pushNavEntry: (SheetRoutes) -> Unit,
    ) {
        if (lastNavEntry is SheetRoutes.RouteStop && filters.stopId == lastNavEntry.stopId) {
            if (
                lastNavEntry.routeStopFilter == filters.routeStopFilter &&
                    lastNavEntry.tripFilter == filters.tripFilter
            ) {
                return
            } else if (lastNavEntry.routeStopFilter == filters.routeStopFilter) {
                setTripFilter(
                    lastNavEntry,
                    filters.stopId,
                    filters.tripFilter,
                    popLastNavEntry,
                    pushNavEntry,
                )
                return
            } else if (lastNavEntry.tripFilter == filters.tripFilter) {
                setRouteStopFilter(
                    lastNavEntry,
                    filters.stopId,
                    filters.routeStopFilter,
                    popLastNavEntry,
                    pushNavEntry,
                )
                return
            }

            if (shouldPopLastStopEntry(lastNavEntry.routeStopFilter, filters.routeStopFilter)) {
                popLastNavEntry()
            }
            if (
                shouldSkipRouteStopFilterUpdate(
                    lastNavEntry,
                    lastNavEntry.stopId,
                    filters.routeStopFilter,
                )
            ) {
                return
            }
        }

        pushNavEntry(
            SheetRoutes.RouteStop(filters.stopId, filters.routeStopFilter, filters.tripFilter),
        )
    }

    fun setRouteStopFilter(
        lastNavEntry: SheetRoutes?,
        stopId: String,
        routeStopFilter: RouteStopFilter?,
        popLastNavEntry: () -> Unit,
        pushNavEntry: (SheetRoutes) -> Unit,
    ) {
        if (lastNavEntry is SheetRoutes.StopOverview && stopId == lastNavEntry.stopId) {
            if (routeStopFilter == null) {
                return
            }
            if (shouldPopLastStopEntry(null, routeStopFilter)) {
                popLastNavEntry()
            }
            if (
                shouldSkipRouteStopFilterUpdate(lastNavEntry, lastNavEntry.stopId, routeStopFilter)
            ) {
                return
            }
        } else if (lastNavEntry is SheetRoutes.RouteStop && stopId == lastNavEntry.stopId) {
            if (lastNavEntry.routeStopFilter == routeStopFilter) {
                return
            }
            if (shouldPopLastStopEntry(lastNavEntry.routeStopFilter, routeStopFilter)) {
                popLastNavEntry()
            }
            if (
                shouldSkipRouteStopFilterUpdate(lastNavEntry, lastNavEntry.stopId, routeStopFilter)
            ) {
                return
            }
        }

        if (routeStopFilter != null) {
            pushNavEntry(SheetRoutes.RouteStop(stopId, routeStopFilter, null))
        } else {
            pushNavEntry(SheetRoutes.StopOverview(stopId))
        }
    }

    fun setTripFilter(
        lastNavEntry: SheetRoutes?,
        stopId: String,
        tripFilter: TripDetailsFilter?,
        popLastNavEntry: () -> Unit,
        pushNavEntry: (SheetRoutes) -> Unit,
    ) {
        if (
            lastNavEntry is SheetRoutes.RouteStop &&
                lastNavEntry.stopId == stopId &&
                lastNavEntry.tripFilter != tripFilter
        ) {
            popLastNavEntry()
            pushNavEntry(SheetRoutes.RouteStop(stopId, lastNavEntry.routeStopFilter, tripFilter))
        }
    }

    private fun shouldSkipRouteStopFilterUpdate(
        lastNavEntry: SheetRoutes?,
        newStopId: String,
        newFilter: RouteStopFilter?,
    ): Boolean {
        // If the new filter is null and there is already a null filter in the stack for the same
        // stop ID, we don't want a duplicate unfiltered entry, so skip appending a new one

        return when (lastNavEntry) {
            is SheetRoutes.StopOverview -> {
                lastNavEntry.stopId == newStopId && newFilter == null
            }
            else -> false
        }
    }
}
