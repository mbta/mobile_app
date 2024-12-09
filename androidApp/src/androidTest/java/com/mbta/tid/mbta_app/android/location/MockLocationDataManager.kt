package com.mbta.tid.mbta_app.android.location

import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow

class MockLocationDataManager(location: Location) : LocationDataManager() {
    override val currentLocation = MutableStateFlow(location)
}
