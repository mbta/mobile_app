package com.mbta.tid.mbta_app.android.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.mapbox.maps.GeoJSONSourceData
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mbta.tid.mbta_app.model.LocationType

class MapLayerManager(val map: MapboxMap, context: Context) {
    var routeSourceGenerator: RouteSourceGenerator? = null
    var stopSourceGenerator: StopSourceGenerator? = null
    var routeLayerGenerator: RouteLayerGenerator? = null
    var stopLayerGenerator: StopLayerGenerator? = null

    init {
        for (icon in StopIcons.entries) {
            val drawable = context.resources.getDrawable(icon.drawableId, null)
            // Mapbox doesn't let us draw vectors in symbols, I guess.
            // https://stackoverflow.com/a/10600736
            val bitmap =
                Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )

            val canvas = Canvas(bitmap)
            drawable.bounds = Rect(0, 0, bitmap.width, bitmap.height)
            drawable.draw(canvas)

            map.addImage(icon.name, bitmap)
        }
    }

    fun addSources(
        routeSourceGenerator: RouteSourceGenerator,
        stopSourceGenerator: StopSourceGenerator
    ) {
        this.routeSourceGenerator = routeSourceGenerator
        this.stopSourceGenerator = stopSourceGenerator

        for (source in
            routeSourceGenerator.routeSourceDetails.map(RouteSourceData::buildSource) +
                stopSourceGenerator.stopSources.map(StopSourceData::buildSource)) {
            addSource(source)
        }
    }

    fun addSource(source: GeoJsonSource) {
        this.map.addSource(source)
    }

    fun addLayers(
        routeLayerGenerator: RouteLayerGenerator,
        stopLayerGenerator: StopLayerGenerator
    ) {
        this.routeLayerGenerator = routeLayerGenerator
        this.stopLayerGenerator = stopLayerGenerator

        for (layer in routeLayerGenerator.routeLayers + stopLayerGenerator.stopLayers) {
            if (map.styleLayerExists("puck")) {
                map.addLayerBelow(layer, below = "puck")
            } else {
                map.addLayer(layer)
            }
        }
    }

    fun updateSourceData(routeSourceGenerator: RouteSourceGenerator) {
        this.routeSourceGenerator = routeSourceGenerator

        for (routeSourceDetails in routeSourceGenerator.routeSourceDetails) {
            if (map.styleSourceExists(routeSourceDetails.sourceId)) {
                map.setStyleGeoJSONSourceData(
                    routeSourceDetails.sourceId,
                    "",
                    GeoJSONSourceData(routeSourceDetails.features)
                )
            } else {
                addSource(routeSourceDetails.buildSource())
            }
        }
    }

    fun updateSourceData(stopSourceGenerator: StopSourceGenerator) {
        this.stopSourceGenerator = stopSourceGenerator

        for (stopSource in stopSourceGenerator.stopSources) {
            if (map.styleSourceExists(stopSource.sourceId)) {
                map.setStyleGeoJSONSourceData(
                    stopSource.sourceId,
                    "",
                    GeoJSONSourceData(stopSource.features)
                )
            } else {
                addSource(stopSource.buildSource())
            }
        }
    }

    fun updateStopLayerZoom(zoomLevel: Double) {
        val opacity = if (zoomLevel > StopIcons.stopZoomThreshold) 1.0 else 0.0
        for (layerType in stopLayerTypes) {
            val layerId = StopLayerGenerator.getStopLayerId(layerType)
            map.getLayerAs<SymbolLayer>(layerId)?.iconOpacity(opacity)
        }
    }

    companion object {
        val stopLayerTypes = listOf(LocationType.STOP, LocationType.STATION)
    }
}
