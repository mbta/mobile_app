package com.mbta.tid.mbta_app.android.map

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
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
import com.mbta.tid.mbta_app.map.ColorPalette
import com.mbta.tid.mbta_app.map.RouteFeaturesBuilder
import com.mbta.tid.mbta_app.map.RouteLayerGenerator
import com.mbta.tid.mbta_app.map.StopFeaturesBuilder
import com.mbta.tid.mbta_app.map.StopIcons
import com.mbta.tid.mbta_app.map.StopLayerGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MapLayerManager(val map: MapboxMap, context: Context) {
    init {
        for (icon in StopIcons.all + AlertIcons.all) {
            val drawable = context.resources.getDrawable(drawableByName(icon), null)

            map.addImage(icon, (drawable as BitmapDrawable).bitmap)
        }
    }

    @AnyThread
    suspend fun addSource(source: GeoJsonSource) {
        withContext(Dispatchers.Main) { map.addSource(source) }
    }

    @AnyThread
    suspend fun addLayers(colorPalette: ColorPalette) {
        val layers: List<MapboxLayer> =
            withContext(Dispatchers.Default) {
                RouteLayerGenerator.createAllRouteLayers(colorPalette).map { it.toMapbox() } +
                    StopLayerGenerator.createStopLayers(colorPalette).map { it.toMapbox() }
            }
        withContext(Dispatchers.Main) {
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
    }

    @MainThread
    fun resetPuckPosition() {
        if (map.styleLayerExists("puck")) {
            map.moveStyleLayer("puck", null)
        }
    }

    @AnyThread
    private suspend fun updateSourceData(sourceId: String, data: FeatureCollection) {
        // styleSourceExists is not thread safe, but setStyleGeoJSONSourceData is
        if (withContext(Dispatchers.Main) { map.styleSourceExists(sourceId) }) {
            withContext(Dispatchers.Default) {
                map.setStyleGeoJSONSourceData(
                    sourceId,
                    "",
                    GeoJSONSourceData(checkNotNull(data.features()))
                )
            }
        } else {
            val source = GeoJsonSource.Builder(sourceId).featureCollection(data).build()
            addSource(source)
        }
    }

    suspend fun updateRouteSourceData(routeData: FeatureCollection) {
        updateSourceData(RouteFeaturesBuilder.routeSourceId, routeData)
    }

    suspend fun updateStopSourceData(stopData: FeatureCollection) {
        updateSourceData(StopFeaturesBuilder.stopSourceId, stopData)
    }
}
