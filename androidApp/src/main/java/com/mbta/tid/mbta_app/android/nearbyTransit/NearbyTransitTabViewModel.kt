package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NearbyTransitTabViewModel : ViewModel() {

    private val _stopDetailsFilter = MutableStateFlow<StopDetailsFilter?>(null)
    val stopDetailsFilter: StateFlow<StopDetailsFilter?> = _stopDetailsFilter

    private val _stopDetailsDepartures = MutableStateFlow<StopDetailsDepartures?>(null)
    val stopDetailsDepartures: StateFlow<StopDetailsDepartures?> = _stopDetailsDepartures

    fun setStopDetailsFilter(filter: StopDetailsFilter?) {
        _stopDetailsFilter.value = filter
    }

    fun setStopDetailsDepartures(departures: StopDetailsDepartures?) {
        _stopDetailsDepartures.value = departures
    }
}
