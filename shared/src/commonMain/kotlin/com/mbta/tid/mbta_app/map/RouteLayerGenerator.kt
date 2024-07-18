package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.map.style.Exp
import com.mbta.tid.mbta_app.map.style.LineJoin
import com.mbta.tid.mbta_app.map.style.LineLayer
import com.mbta.tid.mbta_app.map.style.downcastToColor
import com.mbta.tid.mbta_app.model.SegmentAlertState

object RouteLayerGenerator {
    val routeLayerId = "route-layer"
    val shuttledRouteLayerId = "route-layer-shuttled"
    val suspendedRouteLayerId = "route-layer-suspended"
    val alertingBgRouteLayerId = "route-layer-alerting-bg"
    private val closeZoomCutoff = MapDefaults.closeZoomThreshold

    fun getRouteLayerId(routeId: String) = "$routeLayerId-$routeId"

    fun createAllRouteLayers(colorPalette: ColorPalette): List<LineLayer> =
        listOf(createRouteLayer()) +
            // Draw all alerting layers on top so they are not covered by any overlapping route
            // shape
            createAlertingRouteLayers(colorPalette)

    fun createRouteLayer(): LineLayer {
        val layer = baseRouteLayer(routeLayerId)
        layer.lineWidth = Exp.step(Exp.zoom(), Exp(3), Exp(closeZoomCutoff) to Exp(4))
        return layer
    }

    /**
     * Styling applied only to the portions of the lines that are alerting
     *
     * Creates separate layers for shuttle and suspension segments because `LineLayer.lineDasharray`
     * [doesn't support data-driven styling](https://docs.mapbox.com/style-spec/reference/layers/#paint-line-line-dasharray)
     */
    fun createAlertingRouteLayers(colorPalette: ColorPalette): List<LineLayer> {
        val shuttledLayer = baseRouteLayer(shuttledRouteLayerId)
        shuttledLayer.filter =
            Exp.eq(
                Exp.get(RouteFeaturesBuilder.propAlertStateKey),
                Exp(SegmentAlertState.Shuttle.name)
            )
        shuttledLayer.lineWidth = Exp.step(Exp.zoom(), Exp(4), Exp(closeZoomCutoff) to Exp(6))
        shuttledLayer.lineDasharray = listOf(2.0, 1.33)

        val suspendedLayer = baseRouteLayer(suspendedRouteLayerId)
        suspendedLayer.filter =
            Exp.eq(
                Exp.get(RouteFeaturesBuilder.propAlertStateKey),
                Exp(SegmentAlertState.Suspension.name)
            )
        suspendedLayer.lineWidth = Exp.step(Exp.zoom(), Exp(4), Exp(closeZoomCutoff) to Exp(6))
        suspendedLayer.lineDasharray = listOf(1.33, 2.0)
        suspendedLayer.lineColor = Exp(colorPalette.deemphasized).downcastToColor()

        val alertBackgroundLayer = baseRouteLayer(alertingBgRouteLayerId)
        alertBackgroundLayer.filter =
            Exp.`in`(
                Exp.get(RouteFeaturesBuilder.propAlertStateKey),
                Exp.Bare.arrayOf(SegmentAlertState.Suspension.name, SegmentAlertState.Shuttle.name)
            )
        alertBackgroundLayer.lineWidth =
            Exp.step(Exp.zoom(), Exp(8), Exp(closeZoomCutoff) to Exp(10))
        alertBackgroundLayer.lineColor = Exp(colorPalette.fill3).downcastToColor()

        return listOf(alertBackgroundLayer, shuttledLayer, suspendedLayer)
    }

    fun baseRouteLayer(layerId: String): LineLayer {
        val layer = LineLayer(id = layerId, source = RouteFeaturesBuilder.routeSourceId)
        layer.lineColor = Exp.get(RouteFeaturesBuilder.propRouteColor)
        layer.lineJoin = LineJoin.Round
        layer.lineOffset = lineOffsetExp()
        layer.lineSortKey = Exp.get(RouteFeaturesBuilder.propRouteSortKey)
        checkNotNull(layer.id)
        return layer
    }

    /**
     * Hardcoding offsets based on route properties to minimize the occurences of overlapping rail
     * lines when drawn on the map
     */
    private fun lineOffsetExp(): Exp<Number> {
        val maxLineWidth = 6.0

        return Exp.case(
            Exp.`in`(
                Exp.get(RouteFeaturesBuilder.propRouteId),
                Exp.Bare.arrayOf("CR-Lowell", "CR-Fitchburg")
            ) to Exp(0),
            Exp.`in`(
                Exp.get(RouteFeaturesBuilder.propRouteId),
                Exp.Bare.arrayOf("CR-Greenbush", "CR-Kingston", "CR-Middleborough")
            ) to Exp(maxLineWidth * 1.5),
            Exp.eq(Exp.get(RouteFeaturesBuilder.propRouteType), Exp("COMMUTER_RAIL")) to
                Exp(-maxLineWidth),
            Exp.`in`(Exp("Green"), Exp.get(RouteFeaturesBuilder.propRouteId)) to Exp(maxLineWidth),
            // Default to no offset
            fallback = Exp(0)
        )
    }
}
