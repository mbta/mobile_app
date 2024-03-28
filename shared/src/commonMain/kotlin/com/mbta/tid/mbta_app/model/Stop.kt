package com.mbta.tid.mbta_app.model

import io.github.dellisd.spatialk.geojson.Position
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Stop(
    override val id: String,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    @SerialName("location_type") val locationType: LocationType,
    @SerialName("parent_station_id") val parentStationId: String? = null
) : BackendObject {
    val position = Position(latitude = latitude, longitude = longitude)
}
