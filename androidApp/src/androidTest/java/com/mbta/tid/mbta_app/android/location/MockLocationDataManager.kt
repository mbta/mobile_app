package com.mbta.tid.mbta_app.android.location

import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import org.maplibre.spatialk.geojson.Position

class MockLocationDataManager(position: Position? = Position(latitude = 0.0, longitude = 0.0)) :
    LocationDataManager() {
    override val currentLocation = MutableStateFlow(position?.let { MockLocation(it) })

    fun moveTo(position: Position?) {
        currentLocation.value = position?.let { MockLocation(it) }
    }

    data class MockLocation(val coordinates: Position) : Location("mock") {
        override fun getLatitude() = coordinates.latitude

        override fun getLongitude() = coordinates.longitude
    }
}
