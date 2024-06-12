package com.mbta.tid.mbta_app.android.map

import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.turf.TurfMisc
import com.mbta.tid.mbta_app.android.util.toPoint
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.Stop

data class StopFeatureData(val stop: Stop, val feature: Feature)

data class StopSourceData(
    val sourceId: String,
    val type: LocationType,
    val features: List<Feature>
) {
    fun buildSource() =
        GeoJsonSource.Builder(sourceId)
            .featureCollection(FeatureCollection.fromFeatures(features))
            .build()
}

class StopSourceGenerator(
    val stops: Map<String, Stop>,
    val routeSourceDetails: List<RouteSourceData>?,
) {
    val stopSources =
        kotlin.run {
            val touchedStopIds = mutableSetOf<String>()
            val stopFeatures =
                generateRouteAssociatedStops(stops, routeSourceDetails, touchedStopIds) +
                    generateRemainingStops(stops, touchedStopIds)
            generateStopSources(stopFeatures)
        }

    companion object {
        val stopSourceId = "stop-source"

        fun getStopSourceId(locationType: LocationType) = "$stopSourceId-${locationType.name}"

        val propIdKey = "id"

        fun generateRouteAssociatedStops(
            stops: Map<String, Stop>,
            routeSourceDetails: List<RouteSourceData>?,
            touchedStopIds: MutableSet<String>
        ) =
            routeSourceDetails?.flatMap { routeSource ->
                routeSource.lines.flatMap { lineData ->
                    lineData.stopIds.mapNotNull(
                        fun(childStopId: String): StopFeatureData? {
                            val stopOnRoute = stops[childStopId] ?: return null
                            val stop = stopOnRoute.resolveParent(stops)
                            if (touchedStopIds.contains(stop.id)) return null

                            val snappedCoord =
                                TurfMisc.nearestPointOnLine(
                                    stop.position.toPoint(),
                                    lineData.line.coordinates()
                                )
                            touchedStopIds.add(stop.id)
                            return StopFeatureData(stop, generateStopFeature(stop, snappedCoord))
                        }
                    )
                }
            }
                ?: emptyList()

        fun generateRemainingStops(stops: Map<String, Stop>, touchedStopIds: MutableSet<String>) =
            stops.values.mapNotNull(
                fun(stop: Stop): StopFeatureData? {
                    if (touchedStopIds.contains(stop.id)) return null
                    if (stop.parentStationId != null) return null

                    touchedStopIds.add(stop.id)
                    return StopFeatureData(stop, generateStopFeature(stop))
                }
            )

        private fun generateStopFeature(
            stop: Stop,
            location: Feature = Feature.fromGeometry(stop.position.toPoint()),
        ): Feature =
            Feature.fromGeometry(location.geometry(), JsonObject(), stop.id).apply {
                addStringProperty(propIdKey, stop.id)
            }

        fun generateStopSources(stopFeatures: List<StopFeatureData>): List<StopSourceData> =
            stopFeatures
                .groupBy { it.stop.locationType }
                .map { (type, featureData) ->
                    StopSourceData(getStopSourceId(type), type, featureData.map { it.feature })
                }
    }
}
