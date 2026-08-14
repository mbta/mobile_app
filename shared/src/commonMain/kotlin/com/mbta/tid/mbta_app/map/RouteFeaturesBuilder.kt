package com.mbta.tid.mbta_app.map

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.map.style.Feature
import com.mbta.tid.mbta_app.map.style.FeatureCollection
import com.mbta.tid.mbta_app.map.style.FeatureProperty
import com.mbta.tid.mbta_app.map.style.buildFeatureProperties
import com.mbta.tid.mbta_app.model.AlertAssociatedStop
import com.mbta.tid.mbta_app.model.AlertAwareRouteSegment
import com.mbta.tid.mbta_app.model.GlobalMapData
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteSegment
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SegmentAlertState
import com.mbta.tid.mbta_app.model.SegmentedRouteShape
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.greenRoutes
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.model.response.ShapeWithStops
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import com.mbta.tid.mbta_app.utils.resolveParentId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.turf.measurement.length
import org.maplibre.spatialk.turf.misc.nearestPointTo
import org.maplibre.spatialk.turf.misc.slice
import org.maplibre.spatialk.units.Length

public data class RouteLineData
internal constructor(
    val id: String,
    val sourceRoutePatternId: String,
    val line: LineString,
    val stopIds: List<String>,
    val alertState: SegmentAlertState,
)

public data class RouteSourceData
internal constructor(
    val routeId: LineOrRoute.Id,
    val lines: List<RouteLineData>,
    val features: FeatureCollection,
)

public object RouteFeaturesBuilder {
    internal val routeSourceId = "route-source"

    public fun getRouteSourceId(routeId: LineOrRoute.Id): String = "$routeSourceId-$routeId"

    internal val propAlertStateKey = FeatureProperty<String>("alertState")

    @DefaultArgumentInterop.Enabled
    public suspend fun generateRouteSources(
        routeData: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        globalData: GlobalResponse,
        globalMapData: GlobalMapData?,
        coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): List<RouteSourceData> =
        generateRouteSources(
            routeData,
            globalData,
            globalMapData?.alertsByStop.orEmpty(),
            coroutineDispatcher,
        )

    @DefaultArgumentInterop.Enabled
    internal suspend fun generateRouteSources(
        routeData: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        globalData: GlobalResponse,
        alertsByStop: Map<String, AlertAssociatedStop>,
        coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): List<RouteSourceData> =
        withContext(coroutineDispatcher) {
            routeData.mapNotNull {
                // We can't use getLineOrRoute for Route.Ids, because for the Green Line that will
                // fetch the Line object even when a Route.Id is passed in
                val route =
                    when (it.routeId) {
                        is Line.Id -> globalData.getLineOrRoute(it.routeId)
                        is Route.Id ->
                            globalData.getRoute(it.routeId)?.let { id -> LineOrRoute.Route(id) }
                    }
                route?.let { route ->
                    generateRouteSource(
                        route,
                        routeShapes = it.segmentedShapes,
                        stopsById = globalData.stops,
                        alertsByStop,
                    )
                }
            }
        }

    private fun generateRouteSource(
        route: LineOrRoute,
        routeShapes: List<SegmentedRouteShape>,
        stopsById: Map<String, Stop>,
        alertsByStop: Map<String, AlertAssociatedStop>,
    ): RouteSourceData {
        val routeLines = generateRouteLines(route.type, routeShapes, stopsById, alertsByStop)
        val routeFeatures = routeLines.map { lineData ->
            Feature(
                geometry = lineData.line,
                properties =
                    buildFeatureProperties { put(propAlertStateKey, lineData.alertState.name) },
            )
        }
        val featureCollection = FeatureCollection(routeFeatures)
        return RouteSourceData(route.id, routeLines, featureCollection)
    }

    internal fun shapesWithStopsToMapFriendly(
        shapesWithStops: List<ShapeWithStops>,
        stopsById: Map<String, Stop>?,
    ): List<MapFriendlyRouteResponse.RouteWithSegmentedShapes> =
        shapesWithStops.mapNotNull { shapeWithStops ->
            shapeWithStopsToMapFriendly(shapeWithStops, stopsById)
        }

    internal fun shapeWithStopsToMapFriendly(
        shapeWithStops: ShapeWithStops,
        stopsById: Map<String, Stop>?,
    ): MapFriendlyRouteResponse.RouteWithSegmentedShapes? {
        val shape = shapeWithStops.shape ?: return null
        val parentResolvedStops =
            shapeWithStops.stopIds.map { stopsById?.resolveParentId(it) ?: it }
        return MapFriendlyRouteResponse.RouteWithSegmentedShapes(
            routeId = shapeWithStops.routeId,
            segmentedShapes =
                listOf(
                    SegmentedRouteShape(
                        sourceRoutePatternId = shapeWithStops.routePatternId,
                        sourceRouteId = shapeWithStops.routeId,
                        directionId = shapeWithStops.directionId,
                        routeSegments =
                            listOf(
                                RouteSegment(
                                    id = shape.id,
                                    sourceRoutePatternId = shapeWithStops.routePatternId,
                                    sourceRouteId = shapeWithStops.routeId,
                                    stopIds = parentResolvedStops,
                                    otherPatternsByStopId = emptyMap(),
                                )
                            ),
                        shape = shape,
                    )
                ),
        )
    }

    private fun generateRouteLines(
        routeType: RouteType,
        routeShapes: List<SegmentedRouteShape>,
        stopsById: Map<String, Stop>,
        alertsByStop: Map<String, AlertAssociatedStop>,
    ): List<RouteLineData> {
        return routeShapes.flatMap { routePatternShape ->
            routeShapeToLineData(routePatternShape, routeType, stopsById, alertsByStop)
        }
    }

    private fun routeShapeToLineData(
        routePatternShape: SegmentedRouteShape,
        routeType: RouteType,
        stopsById: Map<String, Stop>?,
        alertsByStop: Map<String, AlertAssociatedStop>?,
    ): List<RouteLineData> {
        val polyline = routePatternShape.shape.polyline ?: return emptyList()
        val coordinates = Polyline.decode(polyline)

        val fullLineString = LineString(coordinates)
        val alertAwareSegments =
            routePatternShape.routeSegments.flatMap { segment ->
                segment.splitAlertingSegments(alertsByStop = alertsByStop ?: emptyMap())
            }
        return alertAwareSegments.mapNotNull { segment ->
            routeSegmentToRouteLineData(
                segment,
                fullLineString,
                routeType,
                stopsById,
            )
        }
    }

    private fun routeSegmentToRouteLineData(
        segment: AlertAwareRouteSegment,
        fullLineString: LineString,
        routeType: RouteType,
        stopsById: Map<String, Stop>?,
    ): RouteLineData? {
        val firstStopId = segment.stopIds.firstOrNull() ?: return null
        val firstStop = stopsById?.get(firstStopId) ?: return null
        val lastStopId = segment.stopIds.lastOrNull() ?: return null
        val lastStop = stopsById.get(lastStopId) ?: return null
        val lineSegment =
            when {
                routeType.isSubway() || routeType == RouteType.COMMUTER_RAIL ->
                    fullLineString.slice(start = firstStop.position, stop = lastStop.position)
                // For bus and ferry, we want to draw the full shape at the ends of the route
                segment.isFirst && segment.isLast -> fullLineString
                segment.isFirst -> firstSlice(fullLineString, lastStop.position)
                segment.isLast -> lastSlice(fullLineString, firstStop.position)
                else -> fullLineString.slice(start = firstStop.position, stop = lastStop.position)
            }
        return RouteLineData(
            id = segment.id,
            sourceRoutePatternId = segment.sourceRoutePatternId,
            line = lineSegment,
            stopIds = segment.stopIds,
            alertState = segment.alertState,
        )
    }

    private fun lengthAtPosition(lineString: LineString, position: Position): Length =
        lineString.nearestPointTo(position).properties.location

    private fun firstSlice(lineString: LineString, stopPosition: Position): LineString =
        lineString.slice(start = Length.Zero, stop = lengthAtPosition(lineString, stopPosition))

    private fun lastSlice(lineString: LineString, startPosition: Position): LineString =
        lineString.slice(
            start = lengthAtPosition(lineString, startPosition),
            stop = lineString.length(),
        )

    public fun forRailAtStop(
        stopShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        railShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        globalData: GlobalResponse?,
    ): List<MapFriendlyRouteResponse.RouteWithSegmentedShapes> =
        forRailAtStop(stopShapes, railShapes, globalData?.routes)

    private fun forRailAtStop(
        stopShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        railShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        routesById: Map<Route.Id, Route>?,
    ): List<MapFriendlyRouteResponse.RouteWithSegmentedShapes> {
        val stopRailRouteIds: Set<LineOrRoute.Id> =
            stopShapes
                .filter { routeWithShape ->
                    val routeType =
                        routesById?.get(routeWithShape.routeId)?.type ?: return@filter false
                    return@filter routeType == RouteType.HEAVY_RAIL ||
                        routeType == RouteType.LIGHT_RAIL ||
                        routeType == RouteType.COMMUTER_RAIL
                }
                .map { it.routeId }
                .toSet()
        return railShapes.filter { stopRailRouteIds.contains(it.routeId) }
    }

    public fun filteredRouteShapesForStop(
        stopMapData: StopMapResponse,
        filter: StopDetailsFilter,
        routeCardData: List<RouteCardData>?,
    ): List<MapFriendlyRouteResponse.RouteWithSegmentedShapes> {
        /**
         * TODO: When we switch to a more involved filter and pinning ID type system, this should be
         *   changed to be less hard coded and do this for any line (we'll then need to figure out
         *   how to get corresponding route ids for each)
         */
        val filterRoutes =
            when (filter.routeId) {
                is Route.Id -> setOf(filter.routeId)
                Line.Id("line-Green") -> greenRoutes
                else -> setOf(filter.routeId)
            }
        val targetRouteData = stopMapData.routeShapes.filter { filterRoutes.contains(it.routeId) }

        if (targetRouteData.isNotEmpty()) {
            return routeCardData?.let {
                val targetRoutePatternIds =
                    routeCardData
                        .asSequence()
                        .flatMap { it.stopData }
                        .flatMap { it.data }
                        .flatMap { it.upcomingTrips }
                        .map { it.trip.routePatternId }
                        .toSet()
                targetRouteData.map { routeData ->
                    val filteredShapes =
                        routeData.segmentedShapes.filter {
                            it.directionId == filter.directionId &&
                                targetRoutePatternIds.contains(it.sourceRoutePatternId)
                        }
                    MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                        routeData.routeId,
                        filteredShapes,
                    )
                }
            }
                ?: targetRouteData.map { routeData ->
                    val filteredShapes =
                        routeData.segmentedShapes.filter { it.directionId == filter.directionId }
                    MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                        routeData.routeId,
                        filteredShapes,
                    )
                }
        }
        return listOf(
            MapFriendlyRouteResponse.RouteWithSegmentedShapes(filter.routeId, emptyList())
        )
    }
}
