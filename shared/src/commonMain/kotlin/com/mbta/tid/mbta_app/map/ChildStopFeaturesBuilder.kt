package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.map.style.Feature
import com.mbta.tid.mbta_app.map.style.FeatureCollection
import com.mbta.tid.mbta_app.map.style.FeatureProperties
import com.mbta.tid.mbta_app.map.style.FeatureProperty
import com.mbta.tid.mbta_app.map.style.buildFeatureProperties
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.Stop
import org.maplibre.spatialk.geojson.Point

internal object ChildStopFeaturesBuilder {
    val childStopSourceId = "child-stop-source"

    val propNameKey = FeatureProperty<String>("name")
    val propLocationTypeKey = FeatureProperty<String>("locationType")
    val propSortOrderKey = FeatureProperty<Number>("sortOrder")

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
        val feature =
            Feature(
                id = childStop.id,
                geometry = Point(childStop.position),
                properties = generateChildStopProperties(childStop, index) ?: return null,
            )
        return feature
    }

    fun generateChildStopProperties(childStop: Stop, index: Int): FeatureProperties? =
        buildFeatureProperties {
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
