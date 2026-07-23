package com.mbta.tid.mbta_app.android.map

import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider

class PassthroughLocationProvider : LocationProvider {
    private var consumers = setOf<LocationConsumer>()

    fun sendLocation(location: Point) {
        for (consumer in consumers) {
            consumer.onLocationUpdated(location)
        }
    }

    override fun registerLocationConsumer(locationConsumer: LocationConsumer) {
        consumers += locationConsumer
    }

    override fun unRegisterLocationConsumer(locationConsumer: LocationConsumer) {
        consumers -= locationConsumer
    }
}
