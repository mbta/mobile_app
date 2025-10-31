package com.mbta.tid.mbta_app.android.map

import com.google.gson.JsonArray as MapboxJsonArray
import com.google.gson.JsonElement as MapboxJsonElement
import com.google.gson.JsonObject as MapboxJsonObject
import com.google.gson.JsonPrimitive as MapboxJsonPrimitive
import com.mapbox.geojson.Feature as MapboxFeature
import com.mapbox.geojson.FeatureCollection as MapboxFeatureCollection
import com.mapbox.geojson.Geometry as MapboxGeometry
import com.mapbox.geojson.GeometryCollection as MapboxGeometryCollection
import com.mapbox.geojson.LineString as MapboxLineString
import com.mapbox.geojson.MultiLineString as MapboxMultiLineString
import com.mapbox.geojson.MultiPoint as MapboxMultiPoint
import com.mapbox.geojson.MultiPolygon as MapboxMultiPolygon
import com.mapbox.geojson.Point as MapboxPoint
import com.mapbox.geojson.Polygon as MapboxPolygon
import com.mapbox.maps.extension.style.expressions.generated.Expression as MapboxExpression
import com.mapbox.maps.extension.style.layers.generated.LineLayer as MapboxLineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer as MapboxSymbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin.Companion as MapboxLineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.TextJustify.Companion as MapboxTextJustify
import com.mbta.tid.mbta_app.android.util.toPoint
import com.mbta.tid.mbta_app.map.style.Exp
import com.mbta.tid.mbta_app.map.style.Feature
import com.mbta.tid.mbta_app.map.style.FeatureCollection
import com.mbta.tid.mbta_app.map.style.JSONArray
import com.mbta.tid.mbta_app.map.style.JSONObject
import com.mbta.tid.mbta_app.map.style.JSONValue
import com.mbta.tid.mbta_app.map.style.LineJoin
import com.mbta.tid.mbta_app.map.style.LineLayer
import com.mbta.tid.mbta_app.map.style.SymbolLayer
import com.mbta.tid.mbta_app.map.style.TextAnchor
import com.mbta.tid.mbta_app.map.style.TextJustify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.GeometryCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.MultiLineString
import org.maplibre.spatialk.geojson.MultiPoint
import org.maplibre.spatialk.geojson.MultiPolygon
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.Position

fun Position.toMapbox() = toPoint()

@JvmName("listPositionToMapbox") private fun List<Position>.toMapbox() = map { it.toMapbox() }

@JvmName("listListPositionToMapbox")
private fun List<List<Position>>.toMapbox() = map { it.toMapbox() }

private fun Geometry.toMapbox(): MapboxGeometry =
    when (this) {
        is GeometryCollection<*> ->
            MapboxGeometryCollection.fromGeometries(this.geometries.map { it.toMapbox() })
        is Point -> MapboxPoint.fromLngLat(this.coordinates.longitude, this.coordinates.latitude)
        is MultiPoint -> MapboxMultiPoint.fromLngLats(this.coordinates.toMapbox())
        is LineString -> MapboxLineString.fromLngLats(this.coordinates.toMapbox())
        is MultiLineString -> MapboxMultiLineString.fromLngLats(this.coordinates.toMapbox())
        is Polygon -> MapboxPolygon.fromLngLats(this.coordinates.toMapbox())
        is MultiPolygon -> MapboxMultiPolygon.fromLngLats(this.coordinates.map { it.toMapbox() })
    }

private fun JSONArray.toMapbox(): MapboxJsonArray =
    MapboxJsonArray().apply {
        for (el in this@toMapbox) {
            add(el.toMapbox())
        }
    }

private fun JSONObject.toMapbox(): MapboxJsonObject =
    MapboxJsonObject().apply {
        for (el in this@toMapbox) {
            add(el.key, el.value.toMapbox())
        }
    }

private fun JSONValue.toMapbox(): MapboxJsonElement =
    when (this) {
        is JSONValue.Array -> this.data.toMapbox()
        is JSONValue.Boolean -> MapboxJsonPrimitive(this.data)
        is JSONValue.Number -> MapboxJsonPrimitive(this.data)
        is JSONValue.Object -> this.data.toMapbox()
        is JSONValue.String -> MapboxJsonPrimitive(this.data)
    }

private fun Feature.toMapbox(): MapboxFeature =
    MapboxFeature.fromGeometry(this.geometry.toMapbox(), this.properties.data.toMapbox(), this.id)

suspend fun FeatureCollection.toMapbox(): MapboxFeatureCollection =
    withContext(Dispatchers.Default) {
        MapboxFeatureCollection.fromFeatures(this@toMapbox.features.map { it.toMapbox() })
    }

private fun <T> Exp<T>.toMapbox() = MapboxExpression.fromRaw(this.toJsonString())

private fun LineJoin.toMapbox() =
    when (this) {
        LineJoin.Bevel -> MapboxLineJoin.BEVEL
        LineJoin.Round -> MapboxLineJoin.ROUND
        LineJoin.Miter -> MapboxLineJoin.MITER
        LineJoin.None -> null
    }

private fun TextAnchor.toMapbox() = this.name.lowercase().replace("_", "-")

private fun TextJustify.toMapbox() =
    when (this) {
        TextJustify.AUTO -> MapboxTextJustify.AUTO
        TextJustify.LEFT -> MapboxTextJustify.LEFT
        TextJustify.CENTER -> MapboxTextJustify.CENTER
        TextJustify.RIGHT -> MapboxTextJustify.RIGHT
    }

suspend fun LineLayer.toMapbox(): MapboxLineLayer =
    withContext(Dispatchers.Default) {
        val result = MapboxLineLayer(layerId = this@toMapbox.id, sourceId = this@toMapbox.source)

        filter?.let { result.filter(it.toMapbox()) }
        minZoom?.let { result.minZoom(it) }

        lineColor?.let { result.lineColor(it.toMapbox()) }
        lineDasharray?.let { result.lineDasharray(it) }
        lineJoin?.toMapbox()?.let { result.lineJoin(it) }
        lineOffset?.let { result.lineOffset(it.toMapbox()) }
        lineSortKey?.let { result.lineSortKey(it.toMapbox()) }
        lineWidth?.let { result.lineWidth(it.toMapbox()) }

        result
    }

suspend fun SymbolLayer.toMapbox(): MapboxSymbolLayer =
    withContext(Dispatchers.Default) {
        val result = MapboxSymbolLayer(layerId = this@toMapbox.id, sourceId = this@toMapbox.source)

        filter?.let { result.filter(it.toMapbox()) }
        minZoom?.let { result.minZoom(it) }

        iconAllowOverlap?.let { result.iconAllowOverlap(it) }
        iconImage?.let { result.iconImage(it.toMapbox()) }
        iconOffset?.let { result.iconOffset(it.toMapbox()) }
        iconPadding?.let { result.iconPadding(it) }
        iconSize?.let { result.iconSize(it.toMapbox()) }

        symbolSortKey?.let { result.symbolSortKey(it.toMapbox()) }

        textAllowOverlap?.let { result.textAllowOverlap(it) }
        textColor?.let { result.textColor(it.toMapbox()) }
        textField?.let { result.textField(it.toMapbox()) }
        textFont?.let { result.textFont(it) }
        textHaloColor?.let { result.textHaloColor(it.toMapbox()) }
        textHaloWidth?.let { result.textHaloWidth(it) }
        textJustify?.let { result.textJustify(it.toMapbox()) }
        textOffset?.let { result.textOffset(it.toMapbox()) }
        textOptional?.let { result.textOptional(it) }
        textRadialOffset?.let { result.textRadialOffset(it) }
        textSize?.let { result.textSize(it) }
        textVariableAnchor?.let { result.textVariableAnchor(it.map(TextAnchor::toMapbox)) }

        result
    }
