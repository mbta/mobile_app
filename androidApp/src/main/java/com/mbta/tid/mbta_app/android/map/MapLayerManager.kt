package com.mbta.tid.mbta_app.android.map

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.GeoJSONSourceData
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.Layer as MapboxLayer
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.SlotLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mbta.tid.mbta_app.android.generated.drawableByName
import com.mbta.tid.mbta_app.map.AlertIcons
import com.mbta.tid.mbta_app.map.ColorPalette
import com.mbta.tid.mbta_app.map.RouteFeaturesBuilder
import com.mbta.tid.mbta_app.map.RouteLayerGenerator
import com.mbta.tid.mbta_app.map.RouteSourceData
import com.mbta.tid.mbta_app.map.StopFeaturesBuilder
import com.mbta.tid.mbta_app.map.StopIcons
import com.mbta.tid.mbta_app.map.StopLayerGenerator
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MapLayerManager(val map: MapboxMap, context: Context) {
    init {
        for (icon in StopIcons.all + AlertIcons.all) {
            val drawable = context.resources.getDrawable(drawableByName(icon), null)

            map.addImage(icon, (drawable as BitmapDrawable).bitmap)
        }
    }

    suspend fun addSource(source: GeoJsonSource) {
        withContext(Dispatchers.Main) {
            try {
                map.addSource(source)
            } catch (error: Exception) {
                Log.e(
                    "MapLayerManager",
                    "Failed to add source (${source.sourceId}):\n${error.localizedMessage}"
                )
            }
        }
    }

    suspend fun addLayers(
        mapFriendlyRouteResponse: MapFriendlyRouteResponse,
        globalResponse: GlobalResponse,
        colorPalette: ColorPalette
    ) {
        addLayers(mapFriendlyRouteResponse.routesWithSegmentedShapes, globalResponse, colorPalette)
    }

    suspend fun addLayers(
        routes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        globalResponse: GlobalResponse,
        colorPalette: ColorPalette
    ) {
        val routeLayers =
            RouteLayerGenerator.createAllRouteLayers(routes, globalResponse, colorPalette).map {
                it.toMapbox()
            }
        val stopLayers = StopLayerGenerator.createStopLayers(colorPalette).map { it.toMapbox() }
        setLayers(routeLayers, stopLayers)
    }

    private suspend fun setLayers(routeLayers: List<MapboxLayer>, stopLayers: List<MapboxLayer>) {
        withContext(Dispatchers.Main) {
            if (!map.styleLayerExists(bufferLayerId)) {
                val layer = SlotLayer(bufferLayerId)
                if (map.styleLayerExists("puck")) {
                    map.addLayerBelow(layer, below = "puck")
                } else {
                    map.addLayer(layer)
                }
            }
            val oldLayers = map.styleLayers.mapTo(mutableSetOf()) { it.id }
            for (layer in routeLayers) {
                oldLayers.remove(layer.layerId)
                if (map.styleLayerExists(checkNotNull(layer.layerId))) {
                    // Skip attempting to add layer if it already exists
                    continue
                }
                map.addLayerBelow(layer, below = bufferLayerId)
            }
            for (layer in stopLayers) {
                oldLayers.remove(layer.layerId)
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
            for (layer in oldLayers) {
                if (layer.startsWith(RouteLayerGenerator.routeLayerId)) {
                    map.removeStyleLayer(layer)
                }
            }
        }
    }

    fun resetPuckPosition() {
        if (map.styleLayerExists("puck")) {
            map.moveStyleLayer("puck", null)
        }
    }

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

    suspend fun updateRouteSourceData(routeData: List<RouteSourceData>) {
        for (data in routeData) {
            updateSourceData(
                RouteFeaturesBuilder.getRouteSourceId(data.routeId),
                data.features.toMapbox()
            )
        }
    }

    suspend fun updateStopSourceData(stopData: FeatureCollection) {
        updateSourceData(StopFeaturesBuilder.stopSourceId, stopData)
    }

    companion object {
        private val bufferLayerId = "empty-layer-between-routes-and-stops"
    }
}
