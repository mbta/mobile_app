package com.mbta.tid.mbta_app.map

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.map.style.Feature
import com.mbta.tid.mbta_app.map.style.FeatureCollection
import com.mbta.tid.mbta_app.map.style.FeatureProperty
import com.mbta.tid.mbta_app.map.style.buildFeatureProperties
import com.mbta.tid.mbta_app.model.MapStop
import com.mbta.tid.mbta_app.model.MapStopRoute
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import io.github.dellisd.spatialk.turf.nearestPointOnLine

data class StopFeatureData(val stop: MapStop, val feature: Feature)

data class StopSourceData
@DefaultArgumentInterop.Enabled
constructor(val filteredStopIds: List<String>? = null, val selectedStopId: String? = null)

object StopFeaturesBuilder {
    val stopSourceId = "stop-source"

    val propIdKey = FeatureProperty<String>("id")
    val propIsSelectedKey = FeatureProperty<Boolean>("isSelected")
    val propIsTerminalKey = FeatureProperty<Boolean>("isTerminal")
    // Map routes is an array of MapStopRoute enum names
    val propMapRoutesKey = FeatureProperty<List<String>>("mapRoutes")
    val propNameKey = FeatureProperty<String>("name")
    // Route IDs are in a map keyed by MapStopRoute enum names, each with a list of IDs
    val propRouteIdsKey = FeatureProperty<Map<String, List<String>>>("routeIds")
    val propServiceStatusKey = FeatureProperty<Map<String, String>>("serviceStatus")
    val propSortOrderKey = FeatureProperty<Number>("sortOrder")

    fun buildCollection(
        stopData: StopSourceData,
        stops: Map<String, MapStop>,
        linesToSnap: List<RouteLineData>
    ): FeatureCollection {
        val filteredStops =
            if (stopData.filteredStopIds != null) {
                stops.filter { stopData.filteredStopIds.contains(it.key) }
            } else {
                stops
            }
        val stopFeatures = generateStopFeatures(stopData, filteredStops, linesToSnap)
        return buildCollection(stopFeatures = stopFeatures)
    }

    fun buildCollection(stopFeatures: List<StopFeatureData>): FeatureCollection {
        return FeatureCollection(features = stopFeatures.map { it.feature })
    }

    private fun generateStopFeatures(
        stopData: StopSourceData,
        stops: Map<String, MapStop>,
        linesToSnap: List<RouteLineData>
    ): List<StopFeatureData> {
        val touchedStopIds: MutableSet<String> = mutableSetOf()
        val routeStops = generateRouteAssociatedStops(stopData, stops, linesToSnap, touchedStopIds)
        val otherStops = generateRemainingStops(stopData, stops, touchedStopIds)
        return otherStops + routeStops
    }

    @OptIn(ExperimentalTurfApi::class)
    private fun generateRouteAssociatedStops(
        stopData: StopSourceData,
        stops: Map<String, MapStop>,
        linesToSnap: List<RouteLineData>,
        touchedStopIds: MutableSet<String>
    ): List<StopFeatureData> {
        return linesToSnap.flatMap { lineData ->
            lineData.stopIds.mapNotNull { childStopId ->
                val stopOnRoute = stops[childStopId] ?: return@mapNotNull null
                val mapStop = stops[stopOnRoute.stop.parentStationId ?: ""] ?: stopOnRoute
                val stop = mapStop.stop

                if (touchedStopIds.contains(stop.id) || mapStop.routeTypes.isEmpty()) {
                    return@mapNotNull null
                }

                val snappedCoord =
                    nearestPointOnLine(line = lineData.line, point = stop.position).point
                touchedStopIds.add(stop.id)
                return@mapNotNull StopFeatureData(
                    stop = mapStop,
                    feature =
                        generateStopFeature(mapStop, stopData, overrideLocation = snappedCoord)
                )
            }
        }
    }

    private fun generateRemainingStops(
        stopData: StopSourceData,
        stops: Map<String, MapStop>,
        touchedStopIds: MutableSet<String>
    ): List<StopFeatureData> {
        return stops.values.mapNotNull { mapStop ->
            val stop = mapStop.stop
            if (
                touchedStopIds.contains(stop.id) ||
                    mapStop.routeTypes.isEmpty() ||
                    stop.parentStationId != null
            ) {
                return@mapNotNull null
            }

            touchedStopIds.add(stop.id)
            return@mapNotNull StopFeatureData(
                stop = mapStop,
                feature = generateStopFeature(mapStop, stopData)
            )
        }
    }

    private fun generateStopFeature(
        mapStop: MapStop,
        stopData: StopSourceData,
        overrideLocation: Position? = null
    ): Feature {
        val stop = mapStop.stop
        return Feature(
            id = stop.id,
            geometry = Point(overrideLocation ?: stop.position),
            properties = generateStopFeatureProperties(mapStop, stopData),
        )
    }

    private fun generateStopFeatureProperties(mapStop: MapStop, stopData: StopSourceData) =
        buildFeatureProperties {
            val stop = mapStop.stop
            put(propIdKey, stop.id)
            put(propNameKey, stop.name)
            put(propIsSelectedKey, stop.id == stopData.selectedStopId)
            put(propIsTerminalKey, mapStop.isTerminal)
            put(propMapRoutesKey, mapStop.routeTypes.map { it.name })
            put(
                propRouteIdsKey,
                mapStop.routes
                    .map { (routeType, routes) -> Pair(routeType.name, routes.map { it.id }) }
                    .toMap()
            )
            put(
                propServiceStatusKey,
                mapStop.alerts
                    .orEmpty()
                    .map { (routeType, alertState) -> Pair(routeType.name, alertState.name) }
                    .toMap()
            )

            // The symbolSortKey must be ascending, so higher priority icons need higher values.
            // This takes the ordinal of the top route and makes it negative. If there are no routes
            // it's set to the total number of route types so that the weird routeless stop is below
            // everything else, and if it has multiple route types, it's set to positive 1 to put
            // the route container above everything else.
            val topRouteOrdinal =
                if (mapStop.routeTypes.isEmpty()) (MapStopRoute.entries.size)
                else mapStop.routeTypes[0].ordinal
            put(propSortOrderKey, if (mapStop.routeTypes.count() > 1) 1 else -topRouteOrdinal)
        }
}
