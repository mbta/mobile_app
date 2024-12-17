package com.mbta.tid.mbta_app.android.map

import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider

class PassthroughLocationProvider : LocationProvider {
    private val consumers = mutableSetOf<LocationConsumer>()
    private var lastLocation: Point? = null

    fun sendLocation(location: Point) {
        lastLocation = location
        for (consumer in consumers) {
            consumer.onLocationUpdated(location)
        }
    }

    override fun registerLocationConsumer(locationConsumer: LocationConsumer) {
        consumers.add(locationConsumer)
        val cachedLocation = lastLocation
        if (cachedLocation != null) {
            locationConsumer.onLocationUpdated(cachedLocation)
        }
    }

    override fun unRegisterLocationConsumer(locationConsumer: LocationConsumer) {
        consumers.remove(locationConsumer)
    }
}
