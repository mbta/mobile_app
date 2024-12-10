package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.android.state.VehiclesTopic
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NearbyTransitTabViewModel : ViewModel() {

    private val _stopDetailsFilter = MutableStateFlow<StopDetailsFilter?>(null)
    public val stopDetailsFilter: StateFlow<StopDetailsFilter?> = _stopDetailsFilter

    private val _vehiclesSubscriptionTopic = MutableStateFlow<VehiclesTopic?>(null)
    public val vehicleSubscriptionTopic: StateFlow<VehiclesTopic?> = _vehiclesSubscriptionTopic

    fun setStopDetailsFilter(filter: StopDetailsFilter?) {
        _stopDetailsFilter.value = filter
    }

    fun setVehiclesSubscriptionTopic(topic: VehiclesTopic?) {
        _vehiclesSubscriptionTopic.value = topic
    }
}
