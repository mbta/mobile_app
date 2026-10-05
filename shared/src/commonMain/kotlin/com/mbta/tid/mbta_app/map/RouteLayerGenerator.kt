package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.map.style.ArrayType
import com.mbta.tid.mbta_app.map.style.Exp
import com.mbta.tid.mbta_app.map.style.Interpolation
import com.mbta.tid.mbta_app.map.style.LineJoin
import com.mbta.tid.mbta_app.map.style.LineLayer
import com.mbta.tid.mbta_app.map.style.TranslateAnchor
import com.mbta.tid.mbta_app.map.style.downcastToColor
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.SegmentAlertState
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.repositories.Settings
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
        settings: Map<Settings, Boolean>,
    ): List<LineLayer> =
        createAllRouteLayers(routesWithShapes, globalResponse.routes, colorPalette, settings)

    private suspend fun createAllRouteLayers(
        routesWithShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        routesById: Map<Route.Id, Route>,
        colorPalette: ColorPalette,
        settings: Map<Settings, Boolean>,
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

            sortedRoutes.map { createRouteLayer(routesById[it.routeId]!!, settings) } +
                // Draw all alerting layers on top so they are not covered by any overlapping route
                // shape
                sortedRoutes.flatMap {
                    createAlertingRouteLayers(routesById[it.routeId]!!, colorPalette, settings)
                }
        }

    internal fun createRouteLayer(route: Route, settings: Map<Settings, Boolean>): LineLayer {
        val layer = baseRouteLayer(getRouteLayerId(route.id), route, settings)
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
        settings: Map<Settings, Boolean>,
    ): List<LineLayer> {
        val shuttledLayer = baseRouteLayer(getRouteLayerId(route.id, "shuttled"), route, settings)
        shuttledLayer.filter =
            Exp.eq(
                Exp.get(RouteFeaturesBuilder.propAlertStateKey),
                Exp(SegmentAlertState.Shuttle.name),
            )
        shuttledLayer.lineWidth = Exp.step(Exp.zoom(), Exp(4), Exp(closeZoomCutoff) to Exp(6))
        shuttledLayer.lineDasharray = listOf(2.0, 1.33)

        val suspendedLayer = baseRouteLayer(getRouteLayerId(route.id, "suspended"), route, settings)
        suspendedLayer.filter =
            Exp.eq(
                Exp.get(RouteFeaturesBuilder.propAlertStateKey),
                Exp(SegmentAlertState.Suspension.name),
            )
        suspendedLayer.lineWidth = Exp.step(Exp.zoom(), Exp(4), Exp(closeZoomCutoff) to Exp(6))
        suspendedLayer.lineDasharray = listOf(1.33, 2.0)
        suspendedLayer.lineColor = Exp(colorPalette.deemphasized).downcastToColor()

        val alertBackgroundLayer =
            baseRouteLayer(getRouteLayerId(route.id, "alerting-bg"), route, settings)
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

    internal fun baseRouteLayer(
        layerId: String,
        route: Route,
        settings: Map<Settings, Boolean>,
    ): LineLayer {
        val layer =
            LineLayer(id = layerId, source = RouteFeaturesBuilder.getRouteSourceId(route.id))
        layer.lineColor = Exp("#${route.color}").downcastToColor()
        layer.lineJoin = LineJoin.Round
        if (settings[Settings.ShiftingDisabled] != true) {
            if (settings[Settings.ShiftingUseTranslate] == true) {
                layer.lineTranslate =
                    lineOffset(
                        route,
                        settings,
                        consumeShift = { Exp.array(ArrayType.Number, 2, listOf(it, Exp(0))) },
                    )
                layer.lineTranslateAnchor = TranslateAnchor.MAP
            } else {
                layer.lineOffset = lineOffset(route, settings, consumeShift = { it })
            }
        }

        checkNotNull(layer.id)
        return layer
    }

    internal fun <T> lineOffset(
        route: Route,
        settings: Map<Settings, Boolean>,
        consumeShift: (Exp<Number>) -> Exp<T>,
    ): Exp<T> {
        val routeIds = Exp.array(contents = listOf(Exp(route.id.idText)))
        val mapRoutes = Exp.array(contents = listOf(Exp(MapStopRoute.matching(route)?.name ?: "")))
        fun b(scale: Double) =
            consumeShift(MapExp.lineShiftEast(scale, routeIds, mapRoutes, settings))
        return if (settings[Settings.ShiftingScaleWithZoom] == true) {
            Exp.interpolate(
                Interpolation.Linear,
                Exp.zoom(),
                Exp(6) to b(0.5),
                Exp(MapDefaults.midZoomThreshold) to b(0.5),
                Exp(MapDefaults.closeZoomThreshold) to b(1.0),
            )
        } else {
            b(1.0)
        }
    }
}
