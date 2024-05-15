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
    val description: String? = null,
    @SerialName("platform_name") val platformName: String? = null,
    @SerialName("parent_station_id") val parentStationId: String? = null,
    @SerialName("child_stop_ids") val childStopIds: List<String> = emptyList()
) : BackendObject {
    val position = Position(latitude = latitude, longitude = longitude)

    fun resolveParent(stops: Map<String, Stop>): Stop {
        if (this.parentStationId == null) return this
        val parentStation = stops[parentStationId] ?: return this
        return parentStation.resolveParent(stops)
    }

    companion object {
        /**
         * Checks if the given stop IDs (as resolved in [stops]) refer to stops which are the same,
         * have the same parent, or are a parent and child.
         */
        fun equalOrFamily(stopId1: String, stopId2: String, stops: Map<String, Stop>): Boolean {
            if (stopId1 == stopId2) return true
            val stop1 = stops.getValue(stopId1)
            val stop2 = stops.getValue(stopId2)
            val parent1 = stop1.resolveParent(stops)
            val parent2 = stop2.resolveParent(stops)
            return parent1.id == parent2.id
        }
    }
}
