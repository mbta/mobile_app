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
import com.mbta.tid.mbta_app.utils.IMapLayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MapLayerManager(val map: MapboxMap, val context: Context) : IMapLayerManager {

    private val lock = Mutex()

    suspend fun loadImages() =
        lock.withLock {
            withContext(Dispatchers.Main) {
                for (icon in StopIcons.all + AlertIcons.all) {
                    val drawable = context.resources.getDrawable(drawableByName(icon), null)

                    map.addImage(icon, (drawable as BitmapDrawable).bitmap)
                }
            }
        }

    private suspend fun addSource(source: GeoJsonSource) {
        withContext(Dispatchers.Main) {
            try {
                map.addSource(source)
            } catch (error: Exception) {
                Log.e(
                    "MapLayerManager",
                    "Failed to add source (${source.sourceId}):\n${error.localizedMessage}",
                )
            }
        }
    }

    override suspend fun addLayers(
        mapFriendlyRouteResponse: MapFriendlyRouteResponse,
        state: StopLayerGenerator.State,
        globalResponse: GlobalResponse,
        colorPalette: ColorPalette,
    ) {
        addLayers(
            mapFriendlyRouteResponse.routesWithSegmentedShapes,
            state,
            globalResponse,
            colorPalette,
        )
    }

    override suspend fun addLayers(
        routes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        state: StopLayerGenerator.State,
        globalResponse: GlobalResponse,
        colorPalette: ColorPalette,
    ) {
        val routeLayers =
            RouteLayerGenerator.createAllRouteLayers(routes, globalResponse, colorPalette).map {
                it.toMapbox()
            }
        val stopLayers =
            StopLayerGenerator.createStopLayers(colorPalette, state).map { it.toMapbox() }
        setLayers(routeLayers, stopLayers)
    }

    override fun resetPuckPosition() {
        /* no-op */
    }

    private suspend fun setLayers(routeLayers: List<MapboxLayer>, stopLayers: List<MapboxLayer>) {
        lock.withLock {
            withContext(Dispatchers.Main) {
                val oldLayers = map.styleLayers.mapTo(mutableSetOf()) { it.id }
                for (layer in routeLayers) {
                    oldLayers.remove(layer.layerId)
                    if (map.styleLayerExists(checkNotNull(layer.layerId))) {
                        map.removeStyleLayer(layer.layerId)
                    }
                    if (map.styleLayerExists(routeAnchorLayerId)) {
                        map.addLayerBelow(layer, below = routeAnchorLayerId)
                    } else {
                        map.addLayer(layer)
                    }
                }
                for (layer in stopLayers) {
                    oldLayers.remove(layer.layerId)
                    if (map.styleLayerExists(checkNotNull(layer.layerId))) {
                        map.removeStyleLayer(layer.layerId)
                    }
                    if (map.styleLayerExists(stopAnchorLayerId)) {
                        map.addLayerBelow(layer, below = stopAnchorLayerId)
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
    }

    suspend fun setUpAnchorLayers() =
        lock.withLock {
            withContext(Dispatchers.Main) {
                if (!map.styleLayerExists(puckAnchorLayerId)) {
                    map.addLayer(SlotLayer(puckAnchorLayerId))
                }
                if (!map.styleLayerExists(stopAnchorLayerId)) {
                    val stopAnchorLayer = SlotLayer(stopAnchorLayerId)
                    map.addLayerBelow(stopAnchorLayer, puckAnchorLayerId)
                }
                if (!map.styleLayerExists(routeAnchorLayerId)) {
                    val routeAnchorLayer = SlotLayer(routeAnchorLayerId)
                    map.addLayerBelow(routeAnchorLayer, stopAnchorLayerId)
                }
            }
        }

    private suspend fun updateSourceData(sourceId: String, data: FeatureCollection) {
        // styleSourceExists is not thread safe, but setStyleGeoJSONSourceData is
        if (withContext(Dispatchers.Main) { map.styleSourceExists(sourceId) }) {
            withContext(Dispatchers.Default) {
                map.setStyleGeoJSONSourceData(
                    sourceId,
                    "",
                    GeoJSONSourceData(checkNotNull(data.features())),
                )
            }
        } else {
            val source = GeoJsonSource.Builder(sourceId).featureCollection(data).build()
            addSource(source)
        }
    }

    override suspend fun updateRouteSourceData(routeData: List<RouteSourceData>) {
        for (data in routeData) {
            updateSourceData(
                RouteFeaturesBuilder.getRouteSourceId(data.routeId),
                data.features.toMapbox(),
            )
        }
    }

    override suspend fun updateStopSourceData(
        stopData: com.mbta.tid.mbta_app.map.style.FeatureCollection
    ) {
        updateSourceData(StopFeaturesBuilder.stopSourceId, stopData.toMapbox())
    }

    companion object {
        val puckAnchorLayerId = "puck-anchor-layer"
        private val routeAnchorLayerId = "route-anchor-layer"
        private val stopAnchorLayerId = "stop-anchor-layer"
    }
}
