package com.mbta.tid.mbta_app.map

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.map.style.Feature
import com.mbta.tid.mbta_app.map.style.FeatureCollection
import com.mbta.tid.mbta_app.map.style.FeatureProperty
import com.mbta.tid.mbta_app.map.style.buildFeatureProperties
import com.mbta.tid.mbta_app.model.GlobalMapData
import com.mbta.tid.mbta_app.model.MapStop
import com.mbta.tid.mbta_app.model.MapStopRoute
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import io.github.dellisd.spatialk.turf.nearestPointOnLine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class StopFeatureData(val stop: MapStop, val feature: Feature)

public object StopFeaturesBuilder {
    public val stopSourceId: String = "stop-source"

    public val propIdKey: FeatureProperty<String> = FeatureProperty("id")
    internal val propIsTerminalKey = FeatureProperty<Boolean>("isTerminal")
    // Map routes is an array of MapStopRoute enum names
    internal val propMapRoutesKey = FeatureProperty<List<String>>("mapRoutes")
    internal val propNameKey = FeatureProperty<String>("name")
    // Route IDs are in a map keyed by MapStopRoute enum names, each with a list of IDs
    internal val propRouteIdsKey = FeatureProperty<Map<String, List<String>>>("routeIds")
    internal val propServiceStatusKey = FeatureProperty<Map<String, String>>("serviceStatus")
    internal val propSortOrderKey = FeatureProperty<Number>("sortOrder")

    /** Routes and directions are stored as "<route id>/<direction id>". */
    internal val propAllRouteDirectionsKey = FeatureProperty<List<String>>("allRouteDirections")

    @DefaultArgumentInterop.Enabled
    public suspend fun buildCollection(
        globalMapData: GlobalMapData?,
        routeSourceDetails: List<RouteSourceData>,
        coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): FeatureCollection =
        buildCollection(globalMapData?.mapStops.orEmpty(), routeSourceDetails, coroutineDispatcher)

    @DefaultArgumentInterop.Enabled
    internal suspend fun buildCollection(
        stops: Map<String, MapStop>,
        routeSourceDetails: List<RouteSourceData>,
        coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): FeatureCollection =
        withContext(coroutineDispatcher) {
            val stopFeatures = generateStopFeatures(stops, routeSourceDetails)
            buildCollection(stopFeatures = stopFeatures)
        }

    private fun buildCollection(stopFeatures: List<StopFeatureData>): FeatureCollection {
        return FeatureCollection(features = stopFeatures.map { it.feature })
    }

    private fun generateStopFeatures(
        stops: Map<String, MapStop>,
        routeSourceDetails: List<RouteSourceData>,
    ): List<StopFeatureData> {
        val touchedStopIds: MutableSet<String> = mutableSetOf()
        val routeStops = generateRouteAssociatedStops(stops, routeSourceDetails, touchedStopIds)
        val otherStops = generateRemainingStops(stops, touchedStopIds)
        return otherStops + routeStops
    }

    @OptIn(ExperimentalTurfApi::class)
    private fun generateRouteAssociatedStops(
        stops: Map<String, MapStop>,
        routeSourceDetails: List<RouteSourceData>,
        touchedStopIds: MutableSet<String>,
    ): List<StopFeatureData> {
        return routeSourceDetails.flatMap { routeSource ->
            routeSource.lines.flatMap { lineData ->
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
                        feature = generateStopFeature(mapStop, overrideLocation = snappedCoord),
                    )
                }
            }
        }
    }

    private fun generateRemainingStops(
        stops: Map<String, MapStop>,
        touchedStopIds: MutableSet<String>,
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
                feature = generateStopFeature(mapStop),
            )
        }
    }

    private fun generateStopFeature(mapStop: MapStop, overrideLocation: Position? = null): Feature {
        val stop = mapStop.stop
        return Feature(
            id = stop.id,
            geometry = Point(overrideLocation ?: stop.position),
            properties = generateStopFeatureProperties(mapStop),
        )
    }

    private fun generateStopFeatureProperties(mapStop: MapStop) = buildFeatureProperties {
        val stop = mapStop.stop
        put(propIdKey, stop.id)
        put(propNameKey, stop.name)
        put(propIsTerminalKey, mapStop.isTerminal)
        put(propMapRoutesKey, mapStop.routeTypes.map { it.name })
        put(
            propRouteIdsKey,
            mapStop.routes
                .map { (routeType, routes) -> Pair(routeType.name, routes.map { it.id }) }
                .toMap(),
        )
        put(
            propServiceStatusKey,
            mapStop.alerts
                .orEmpty()
                .map { (routeType, alertState) -> Pair(routeType.name, alertState.name) }
                .toMap(),
        )
        put(
            propAllRouteDirectionsKey,
            mapStop.routeDirections.flatMap { (routeId, directions) ->
                directions.map { "$routeId/$it" }
            },
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
