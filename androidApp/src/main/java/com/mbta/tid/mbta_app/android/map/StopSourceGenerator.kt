package com.mbta.tid.mbta_app.android.map

import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.turf.TurfMisc
import com.mbta.tid.mbta_app.android.util.toPoint
import com.mbta.tid.mbta_app.model.AlertAssociatedStop
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopAlertState

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
    val alertsByStop: Map<String, AlertAssociatedStop>?
) {
    val stopSources =
        kotlin.run {
            val touchedStopIds = mutableSetOf<String>()
            val stopFeatures =
                generateRouteAssociatedStops(
                    stops,
                    routeSourceDetails,
                    alertsByStop,
                    touchedStopIds
                ) + generateRemainingStops(stops, alertsByStop, touchedStopIds)
            generateStopSources(stopFeatures)
        }

    companion object {
        val stopSourceId = "stop-source"

        fun getStopSourceId(locationType: LocationType) = "$stopSourceId-${locationType.name}"

        val propIdKey = "id"
        val propServiceStatusKey = "serviceStatus"

        fun generateRouteAssociatedStops(
            stops: Map<String, Stop>,
            routeSourceDetails: List<RouteSourceData>?,
            alertsByStop: Map<String, AlertAssociatedStop>?,
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
                            return StopFeatureData(
                                stop,
                                generateStopFeature(stop, alertsByStop, snappedCoord)
                            )
                        }
                    )
                }
            }
                ?: emptyList()

        fun generateRemainingStops(
            stops: Map<String, Stop>,
            alertsByStop: Map<String, AlertAssociatedStop>?,
            touchedStopIds: MutableSet<String>
        ) =
            stops.values.mapNotNull(
                fun(stop: Stop): StopFeatureData? {
                    if (touchedStopIds.contains(stop.id)) return null
                    if (stop.parentStationId != null) return null

                    touchedStopIds.add(stop.id)
                    return StopFeatureData(stop, generateStopFeature(stop, alertsByStop))
                }
            )

        private fun generateStopFeature(
            stop: Stop,
            alertsByStop: Map<String, AlertAssociatedStop>?,
            location: Feature = Feature.fromGeometry(stop.position.toPoint()),
        ): Feature =
            Feature.fromGeometry(location.geometry(), JsonObject(), stop.id).apply {
                addStringProperty(propIdKey, stop.id)
                addStringProperty(
                    propServiceStatusKey,
                    (alertsByStop?.get(stop.id)?.serviceStatus ?: StopAlertState.Normal).name
                )
            }

        fun generateStopSources(stopFeatures: List<StopFeatureData>): List<StopSourceData> =
            stopFeatures
                .groupBy { it.stop.locationType }
                .map { (type, featureData) ->
                    StopSourceData(getStopSourceId(type), type, featureData.map { it.feature })
                }
    }
}
