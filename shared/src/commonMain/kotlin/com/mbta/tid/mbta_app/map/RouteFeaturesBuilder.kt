package com.mbta.tid.mbta_app.map

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.map.style.Feature
import com.mbta.tid.mbta_app.map.style.FeatureCollection
import com.mbta.tid.mbta_app.map.style.FeatureProperty
import com.mbta.tid.mbta_app.map.style.buildFeatureProperties
import com.mbta.tid.mbta_app.model.AlertAssociatedStop
import com.mbta.tid.mbta_app.model.AlertAwareRouteSegment
import com.mbta.tid.mbta_app.model.GlobalMapData
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
import io.github.dellisd.spatialk.geojson.LineString
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import io.github.dellisd.spatialk.turf.lineSlice
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RouteLineData(
    val id: String,
    val sourceRoutePatternId: String,
    val line: LineString,
    val stopIds: List<String>,
    val alertState: SegmentAlertState,
)

data class RouteSourceData(
    val routeId: String,
    val lines: List<RouteLineData>,
    val features: FeatureCollection,
)

object RouteFeaturesBuilder {
    val routeSourceId = "route-source"

    fun getRouteSourceId(routeId: String) = "$routeSourceId-$routeId"

    val propAlertStateKey = FeatureProperty<String>("alertState")

    @DefaultArgumentInterop.Enabled
    suspend fun generateRouteSources(
        routeData: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        globalData: GlobalResponse,
        globalMapData: GlobalMapData?,
        coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ) =
        generateRouteSources(
            routeData,
            globalData.stops,
            globalMapData?.alertsByStop.orEmpty(),
            coroutineDispatcher,
        )

    @DefaultArgumentInterop.Enabled
    suspend fun generateRouteSources(
        routeData: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        stopsById: Map<String, Stop>,
        alertsByStop: Map<String, AlertAssociatedStop>,
        coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): List<RouteSourceData> =
        withContext(coroutineDispatcher) {
            routeData.map {
                generateRouteSource(
                    routeId = it.routeId,
                    routeShapes = it.segmentedShapes,
                    stopsById,
                    alertsByStop,
                )
            }
        }

    private fun generateRouteSource(
        routeId: String,
        routeShapes: List<SegmentedRouteShape>,
        stopsById: Map<String, Stop>,
        alertsByStop: Map<String, AlertAssociatedStop>,
    ): RouteSourceData {
        val routeLines = generateRouteLines(routeId, routeShapes, stopsById, alertsByStop)
        val routeFeatures =
            routeLines.map { lineData ->
                Feature(
                    geometry = lineData.line,
                    properties =
                        buildFeatureProperties { put(propAlertStateKey, lineData.alertState.name) },
                )
            }
        val featureCollection = FeatureCollection(routeFeatures)
        return RouteSourceData(routeId, routeLines, featureCollection)
    }

    fun shapesWithStopsToMapFriendly(
        shapesWithStops: List<ShapeWithStops>,
        stopsById: Map<String, Stop>?,
    ): List<MapFriendlyRouteResponse.RouteWithSegmentedShapes> =
        shapesWithStops.mapNotNull { shapeWithStops ->
            shapeWithStopsToMapFriendly(shapeWithStops, stopsById)
        }

    fun shapeWithStopsToMapFriendly(
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
        routeId: String,
        routeShapes: List<SegmentedRouteShape>,
        stopsById: Map<String, Stop>,
        alertsByStop: Map<String, AlertAssociatedStop>,
    ): List<RouteLineData> {
        return routeShapes.flatMap { routePatternShape ->
            routeShapeToLineData(routePatternShape, stopsById, alertsByStop)
        }
    }

    private fun routeShapeToLineData(
        routePatternShape: SegmentedRouteShape,
        stopsById: Map<String, Stop>?,
        alertsByStop: Map<String, AlertAssociatedStop>?,
    ): List<RouteLineData> {
        val polyline = routePatternShape.shape.polyline ?: return emptyList()
        val coordinates = Polyline.decode(polyline)

        val fullLineString = LineString(coordinates)
        val alertAwareSegments =
            routePatternShape.routeSegments.flatMap { segment ->
                if (segment.sourceRouteId == "Green-B") {
                    println("Green-B segment found")
                }
                segment.splitAlertingSegments(alertsByStop = alertsByStop ?: emptyMap())
            }
        return alertAwareSegments.mapNotNull { segment ->
            routeSegmentToRouteLineData(
                segment = segment,
                fullLineString = fullLineString,
                stopsById = stopsById,
            )
        }
    }

    @OptIn(ExperimentalTurfApi::class)
    private fun routeSegmentToRouteLineData(
        segment: AlertAwareRouteSegment,
        fullLineString: LineString,
        stopsById: Map<String, Stop>?,
    ): RouteLineData? {
        val firstStopId = segment.stopIds.firstOrNull() ?: return null
        val firstStop = stopsById?.get(firstStopId) ?: return null
        val lastStopId = segment.stopIds.lastOrNull() ?: return null
        val lastStop = stopsById.get(lastStopId) ?: return null
        val lineSegment =
            lineSlice(start = firstStop.position, stop = lastStop.position, line = fullLineString)
        return RouteLineData(
            id = segment.id,
            sourceRoutePatternId = segment.sourceRoutePatternId,
            line = lineSegment,
            stopIds = segment.stopIds,
            alertState = segment.alertState,
        )
    }

    fun forRailAtStop(
        stopShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        railShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        globalData: GlobalResponse?,
    ) = forRailAtStop(stopShapes, railShapes, globalData?.routes)

    private fun forRailAtStop(
        stopShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        railShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        routesById: Map<String, Route>?,
    ): List<MapFriendlyRouteResponse.RouteWithSegmentedShapes> {
        val stopRailRouteIds: Set<String> =
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

    fun filteredRouteShapesForStop(
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
            if (filter.routeId == "line-Green") {
                greenRoutes
            } else {
                setOf(filter.routeId)
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
