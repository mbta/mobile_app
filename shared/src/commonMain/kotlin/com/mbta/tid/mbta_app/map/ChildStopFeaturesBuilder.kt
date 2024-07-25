package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.Stop
import io.github.dellisd.spatialk.geojson.Feature
import io.github.dellisd.spatialk.geojson.FeatureCollection
import io.github.dellisd.spatialk.geojson.Point
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object ChildStopFeaturesBuilder {
    val childStopSourceId = "child-stop-source"

    val propNameKey = "name"
    val propLocationTypeKey = "locationType"
    val propSortOrderKey = "sortOrder"

    fun generateChildStopFeatures(childStops: Map<String, Stop>?): FeatureCollection {
        if (childStops != null) {
            val stopsInOrder = childStops.values.sortedBy { it.id }
            return FeatureCollection(
                stopsInOrder.mapIndexedNotNull { index, stop ->
                    generateChildStopFeature(childStop = stop, index = index)
                }
            )
        } else {
            return FeatureCollection(emptyList())
        }
    }

    fun generateChildStopFeature(childStop: Stop, index: Int): Feature? {
        var feature =
            Feature(
                id = childStop.id,
                geometry = Point(childStop.position),
                properties = generateChildStopProperties(childStop, index) ?: return null
            )
        return feature
    }

    fun generateChildStopProperties(childStop: Stop, index: Int): JsonObject? = buildJsonObject {
        when (childStop.locationType) {
            LocationType.ENTRANCE_EXIT ->
                put(propNameKey, childStop.name.split(" - ").lastOrNull() ?: "")
            LocationType.BOARDING_AREA,
            LocationType.STOP -> put(propNameKey, childStop.platformName ?: childStop.name)
            else -> return null
        }

        put(propLocationTypeKey, childStop.locationType.name)
        put(propSortOrderKey, index)
    }
}
