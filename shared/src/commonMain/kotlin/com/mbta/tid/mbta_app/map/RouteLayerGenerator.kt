package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.map.style.Exp
import com.mbta.tid.mbta_app.map.style.LineJoin
import com.mbta.tid.mbta_app.map.style.LineLayer
import com.mbta.tid.mbta_app.map.style.downcastToColor
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SegmentAlertState
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

public object RouteLayerGenerator {
    public val routeLayerId: String = "route-layer"
    private val closeZoomCutoff = MapDefaults.closeZoomThreshold

    internal fun getRouteLayerId(routeId: Route.Id) = "$routeLayerId-$routeId"

    internal fun getRouteLayerId(routeId: Route.Id, suffix: String) =
        "$routeLayerId-$routeId-$suffix"

    public suspend fun createAllRouteLayers(
        routesWithShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        globalResponse: GlobalResponse,
        colorPalette: ColorPalette,
    ): List<LineLayer> = createAllRouteLayers(routesWithShapes, globalResponse.routes, colorPalette)

    private suspend fun createAllRouteLayers(
        routesWithShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        routesById: Map<Route.Id, Route>,
        colorPalette: ColorPalette,
    ): List<LineLayer> =
        withContext(Dispatchers.Default) {
            val sortedRoutes =
                routesWithShapes
                    .filter { routesById.containsKey(it.routeId) }
                    .sortedBy {
                        // Sort by reverse sort order so that lowest ordered routes are drawn
                        // first/lowest
                        -routesById[it.routeId]!!.sortOrder
                    }

            sortedRoutes.map { createRouteLayer(routesById[it.routeId]!!) } +
                // Draw all alerting layers on top so they are not covered by any overlapping route
                // shape
                sortedRoutes.flatMap {
                    createAlertingRouteLayers(routesById[it.routeId]!!, colorPalette)
                }
        }

    internal fun createRouteLayer(route: Route): LineLayer {
        val layer = baseRouteLayer(getRouteLayerId(route.id), route)
        layer.lineWidth = Exp.step(Exp.zoom(), Exp(3), Exp(closeZoomCutoff) to Exp(4))
        return layer
    }

    /**
     * Styling applied only to the portions of the lines that are alerting
     *
     * Creates separate layers for shuttle and suspension segments because `LineLayer.lineDasharray`
     * [doesn't support data-driven styling](https://docs.mapbox.com/style-spec/reference/layers/#paint-line-line-dasharray)
     */
    internal fun createAlertingRouteLayers(
        route: Route,
        colorPalette: ColorPalette,
    ): List<LineLayer> {
        val shuttledLayer = baseRouteLayer(getRouteLayerId(route.id, "shuttled"), route)
        shuttledLayer.filter =
            Exp.eq(
                Exp.get(RouteFeaturesBuilder.propAlertStateKey),
                Exp(SegmentAlertState.Shuttle.name),
            )
        shuttledLayer.lineWidth = Exp.step(Exp.zoom(), Exp(4), Exp(closeZoomCutoff) to Exp(6))
        shuttledLayer.lineDasharray = listOf(2.0, 1.33)

        val suspendedLayer = baseRouteLayer(getRouteLayerId(route.id, "suspended"), route)
        suspendedLayer.filter =
            Exp.eq(
                Exp.get(RouteFeaturesBuilder.propAlertStateKey),
                Exp(SegmentAlertState.Suspension.name),
            )
        suspendedLayer.lineWidth = Exp.step(Exp.zoom(), Exp(4), Exp(closeZoomCutoff) to Exp(6))
        suspendedLayer.lineDasharray = listOf(1.33, 2.0)
        suspendedLayer.lineColor = Exp(colorPalette.deemphasized).downcastToColor()

        val alertBackgroundLayer = baseRouteLayer(getRouteLayerId(route.id, "alerting-bg"), route)
        alertBackgroundLayer.filter =
            Exp.`in`(
                Exp.get(RouteFeaturesBuilder.propAlertStateKey),
                Exp.Bare.arrayOf(SegmentAlertState.Suspension.name, SegmentAlertState.Shuttle.name),
            )
        alertBackgroundLayer.lineWidth =
            Exp.step(Exp.zoom(), Exp(8), Exp(closeZoomCutoff) to Exp(10))
        alertBackgroundLayer.lineColor = Exp(colorPalette.fill3).downcastToColor()

        return listOf(alertBackgroundLayer, shuttledLayer, suspendedLayer)
    }

    internal fun baseRouteLayer(layerId: String, route: Route): LineLayer {
        val layer =
            LineLayer(id = layerId, source = RouteFeaturesBuilder.getRouteSourceId(route.id))
        layer.lineColor = Exp("#${route.color}")
        layer.lineJoin = LineJoin.Round
        layer.lineOffset = Exp(lineOffset(route))
        checkNotNull(layer.id)
        return layer
    }

    /**
     * Hardcoding offsets based on route properties to minimize the occurences of overlapping rail
     * lines when drawn on the map
     */
    private fun lineOffset(route: Route): Double {
        val maxLineWidth = 6.0
        val greenOverlappingCR = setOf(Route.Id("CR-Lowell"), Route.Id("CR-Fitchburg"))
        val redOverlappingCR =
            setOf(
                Route.Id("CR-Greenbush"),
                Route.Id("CR-Kingston"),
                Route.Id("CR-Middleborough"),
                Route.Id("CR-NewBedford"),
            )

        return if (route.type == RouteType.COMMUTER_RAIL) {
            when {
                greenOverlappingCR.contains(route.id) -> {
                    // These overlap with GL, GL is offset below, so do nothing
                    0.0
                }
                redOverlappingCR.contains(route.id) -> {
                    // These overlap with RL. RL is offset below, shift West
                    maxLineWidth * 1.5
                }
                else -> {
                    // Some overlap with OL and should shift East.
                    // Shift the rest east too so they scale porportionally
                    -maxLineWidth
                }
            }
        } else if (route.id.idText.contains("Green")) {
            // Account for overlapping North Station - Haymarket
            // Offset to the East
            maxLineWidth
        } else {
            0.0
        }
    }
}
