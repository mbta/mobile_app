package com.mbta.tid.mbta_app.android.map

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.GeoJSONSourceData
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.Layer as MapboxLayer
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mbta.tid.mbta_app.android.generated.drawableByName
import com.mbta.tid.mbta_app.map.AlertIcons
import com.mbta.tid.mbta_app.map.ChildStopFeaturesBuilder
import com.mbta.tid.mbta_app.map.ChildStopIcons
import com.mbta.tid.mbta_app.map.ChildStopLayerGenerator
import com.mbta.tid.mbta_app.map.ColorPalette
import com.mbta.tid.mbta_app.map.RouteFeaturesBuilder
import com.mbta.tid.mbta_app.map.RouteLayerGenerator
import com.mbta.tid.mbta_app.map.StopFeaturesBuilder
import com.mbta.tid.mbta_app.map.StopIcons
import com.mbta.tid.mbta_app.map.StopLayerGenerator

class MapLayerManager(val map: MapboxMap, context: Context) {
    init {
        for (icon in StopIcons.all + AlertIcons.all + ChildStopIcons.all) {
            val drawable = context.resources.getDrawable(drawableByName(icon), null)

            map.addImage(icon, (drawable as BitmapDrawable).bitmap)
        }
    }

    fun addSource(source: GeoJsonSource) {
        this.map.addSource(source)
    }

    fun addLayers(colorPalette: ColorPalette) {
        val layers: List<MapboxLayer> =
            RouteLayerGenerator.createAllRouteLayers(colorPalette).map { it.toMapbox() } +
                StopLayerGenerator.createStopLayers(colorPalette).map { it.toMapbox() } +
                listOf(ChildStopLayerGenerator.createChildStopLayer(colorPalette).toMapbox())
        for (layer in layers) {
            if (map.styleLayerExists(checkNotNull(layer.layerId))) {
                // Skip attempting to add layer if it already exists
                continue
            }
            if (map.styleLayerExists("puck")) {
                map.addLayerBelow(layer, below = "puck")
            } else {
                map.addLayer(layer)
            }
        }
    }

    private fun updateSourceData(sourceId: String, data: FeatureCollection) {
        if (map.styleSourceExists(sourceId)) {
            map.setStyleGeoJSONSourceData(
                sourceId,
                "",
                GeoJSONSourceData(checkNotNull(data.features()))
            )
        } else {
            val source = GeoJsonSource.Builder(sourceId).featureCollection(data).build()
            addSource(source)
        }
    }

    fun updateRouteSourceData(routeData: FeatureCollection) {
        updateSourceData(RouteFeaturesBuilder.routeSourceId, routeData)
    }

    fun updateStopSourceData(stopData: FeatureCollection) {
        updateSourceData(StopFeaturesBuilder.stopSourceId, stopData)
    }

    fun updateChildStopSourceData(childStopData: FeatureCollection) {
        updateSourceData(ChildStopFeaturesBuilder.childStopSourceId, childStopData)
    }
}
