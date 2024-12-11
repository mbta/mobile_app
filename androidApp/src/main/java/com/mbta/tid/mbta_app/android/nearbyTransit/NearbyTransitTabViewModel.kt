package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.android.state.RouteDirection
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NearbyTransitTabViewModel : ViewModel() {

    private val _stopDetailsFilter = MutableStateFlow<StopDetailsFilter?>(null)
    val stopDetailsFilter: StateFlow<StopDetailsFilter?> = _stopDetailsFilter

    private val _vehiclesRouteDirection = MutableStateFlow<RouteDirection?>(null)
    val vehiclesRouteDirection: StateFlow<RouteDirection?> = _vehiclesRouteDirection

    fun setStopDetailsFilter(filter: StopDetailsFilter?) {
        _stopDetailsFilter.value = filter
    }

    fun setVehiclesRouteDirection(routeDirection: RouteDirection?) {
        _vehiclesRouteDirection.value = routeDirection
    }
}
