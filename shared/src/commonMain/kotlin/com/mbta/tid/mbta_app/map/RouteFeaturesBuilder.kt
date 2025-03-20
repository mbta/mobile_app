package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.map.style.Color
import com.mbta.tid.mbta_app.map.style.Feature
import com.mbta.tid.mbta_app.map.style.FeatureCollection
import com.mbta.tid.mbta_app.map.style.FeatureProperty
import com.mbta.tid.mbta_app.map.style.buildFeatureProperties
import com.mbta.tid.mbta_app.model.AlertAssociatedStop
import com.mbta.tid.mbta_app.model.AlertAwareRouteSegment
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteSegment
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SegmentAlertState
import com.mbta.tid.mbta_app.model.SegmentedRouteShape
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.put

data class RouteLineData(
    val id: String,
    val routeId: String,
    val route: Route?,
    val routePatternId: String,
    val line: LineString,
    val stopIds: List<String>,
    val alertState: SegmentAlertState
) {
    val routeType = route?.type?.name
    val color = route?.color?.let { "#$it" }
    val sortKey = route?.sortOrder?.unaryMinus() ?: Int.MIN_VALUE
}

object RouteFeaturesBuilder {
    val routeSourceId = "route-source"

    val propRouteId = FeatureProperty<String>("routeId")
    val propRouteType = FeatureProperty<String>("routeType")
    val propRouteSortKey = FeatureProperty<Number>("routeSortKey")
    val propRouteColor = FeatureProperty<Color>("routeColor")
    val propAlertStateKey = FeatureProperty<String>("alertState")

    suspend fun generateRouteLines(
        routeData: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        globalData: GlobalResponse?,
        alertsByStop: Map<String, AlertAssociatedStop>?
    ) = generateRouteLines(routeData, globalData?.routes, globalData?.stops, alertsByStop)

    suspend fun generateRouteLines(
        routeData: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        routesById: Map<String, Route>?,
        stopsById: Map<String, Stop>?,
        alertsByStop: Map<String, AlertAssociatedStop>?
    ): List<RouteLineData> {
        return withContext(Dispatchers.Default) {
            routeData.flatMap {
                generateRouteLines(
                    routeWithShapes = it,
                    route = routesById?.get(it.routeId),
                    stopsById = stopsById,
                    alertsByStop = alertsByStop
                )
            }
        }
    }

    suspend fun buildCollection(
        routeData: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        routesById: Map<String, Route>?,
        stopsById: Map<String, Stop>?,
        alertsByStop: Map<String, AlertAssociatedStop>?
    ): FeatureCollection {
        return withContext(Dispatchers.Default) {
            val routeLines: List<RouteLineData> =
                generateRouteLines(
                    routeData = routeData,
                    routesById = routesById,
                    stopsById = stopsById,
                    alertsByStop = alertsByStop
                )
            buildCollection(routeLines = routeLines)
        }
    }

    fun buildCollection(routeLines: List<RouteLineData>): FeatureCollection {
        val routeFeatures = routeLines.map { lineToFeature(routeLineData = it) }
        return FeatureCollection(routeFeatures)
    }

    fun shapesWithStopsToMapFriendly(
        shapesWithStops: List<ShapeWithStops>,
        stopsById: Map<String, Stop>?
    ): List<MapFriendlyRouteResponse.RouteWithSegmentedShapes> =
        shapesWithStops.mapNotNull { shapeWithStops ->
            shapeWithStopsToMapFriendly(shapeWithStops, stopsById)
        }

    fun shapeWithStopsToMapFriendly(
        shapeWithStops: ShapeWithStops,
        stopsById: Map<String, Stop>?
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
                                    otherPatternsByStopId = emptyMap()
                                ),
                            ),
                        shape = shape
                    ),
                )
        )
    }

    fun lineToFeature(routeLineData: RouteLineData) =
        Feature(
            geometry = routeLineData.line,
            properties =
                buildFeatureProperties {
                    put(propRouteId, routeLineData.routeId)
                    routeLineData.routeType?.let { put(propRouteType, it) }
                    routeLineData.color?.let { put(propRouteColor, it) }
                    put(propRouteSortKey, routeLineData.sortKey)

                    put(propAlertStateKey, routeLineData.alertState.name)
                }
        )

    fun generateRouteLines(
        routeWithShapes: MapFriendlyRouteResponse.RouteWithSegmentedShapes,
        route: Route?,
        stopsById: Map<String, Stop>?,
        alertsByStop: Map<String, AlertAssociatedStop>?
    ): List<RouteLineData> {
        return routeWithShapes.segmentedShapes.flatMap { routePatternShape ->
            routeShapeToLineData(
                routePatternShape = routePatternShape,
                route = route,
                stopsById = stopsById,
                alertsByStop = alertsByStop
            )
        }
    }

    private fun routeShapeToLineData(
        routePatternShape: SegmentedRouteShape,
        route: Route?,
        stopsById: Map<String, Stop>?,
        alertsByStop: Map<String, AlertAssociatedStop>?
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
                segment = segment,
                route = route,
                fullLineString = fullLineString,
                stopsById = stopsById
            )
        }
    }

    @OptIn(ExperimentalTurfApi::class)
    private fun routeSegmentToRouteLineData(
        segment: AlertAwareRouteSegment,
        route: Route?,
        fullLineString: LineString,
        stopsById: Map<String, Stop>?
    ): RouteLineData? {
        val firstStopId = segment.stopIds.firstOrNull() ?: return null
        val firstStop = stopsById?.get(firstStopId) ?: return null
        val lastStopId = segment.stopIds.lastOrNull() ?: return null
        val lastStop = stopsById.get(lastStopId) ?: return null
        val lineSegment =
            lineSlice(start = firstStop.position, stop = lastStop.position, line = fullLineString)
        return RouteLineData(
            id = segment.id,
            routeId = segment.sourceRouteId,
            route = route,
            routePatternId = segment.sourceRoutePatternId,
            line = lineSegment,
            stopIds = segment.stopIds,
            alertState = segment.alertState
        )
    }

    fun forRailAtStop(
        stopShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        railShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        routesById: Map<String, Route>?
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
        departures: StopDetailsDepartures?
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
            return departures?.let {
                val targetRoutePatternIds = departures.upcomingPatternIds
                targetRouteData.map { routeData ->
                    val filteredShapes =
                        routeData.segmentedShapes.filter {
                            it.directionId == filter.directionId &&
                                targetRoutePatternIds.contains(it.sourceRoutePatternId)
                        }
                    MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                        routeData.routeId,
                        filteredShapes
                    )
                }
            }
                ?: targetRouteData.map { routeData ->
                    val filteredShapes =
                        routeData.segmentedShapes.filter { it.directionId == filter.directionId }
                    MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                        routeData.routeId,
                        filteredShapes
                    )
                }
        }
        return listOf(
            MapFriendlyRouteResponse.RouteWithSegmentedShapes(filter.routeId, emptyList())
        )
    }
}
