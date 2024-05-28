package com.mbta.tid.mbta_app.android.map

import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.turf.TurfMisc.lineSlice
import com.mbta.tid.mbta_app.android.util.toPoint
import com.mbta.tid.mbta_app.model.AlertAssociatedStop
import com.mbta.tid.mbta_app.model.AlertAwareRouteSegment
import com.mbta.tid.mbta_app.model.SegmentAlertState
import com.mbta.tid.mbta_app.model.SegmentedRouteShape
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse

data class RouteLineData(
    val id: String,
    val sourceRoutePatternId: String,
    val line: LineString,
    val stopIds: List<String>,
    val alertState: SegmentAlertState
)

data class RouteSourceData(
    val routeId: String,
    val lines: List<RouteLineData>,
    val sourceId: String,
    val features: List<Feature>
) {
    fun buildSource() =
        GeoJsonSource.Builder(sourceId)
            .featureCollection(FeatureCollection.fromFeatures(features))
            .build()
}

class RouteSourceGenerator(
    routeData: MapFriendlyRouteResponse,
    stopsById: Map<String, Stop>,
    alertsByStop: Map<String, AlertAssociatedStop>?
) {
    val routeSourceDetails = generateRouteSources(routeData, stopsById, alertsByStop)

    companion object {
        private val routeSourceId = "route-source"

        fun getRouteSourceId(routeId: String) = "$routeSourceId-$routeId"

        val propAlertStateKey = "isAlerting"

        fun generateRouteSources(
            routeData: MapFriendlyRouteResponse,
            stopsById: Map<String, Stop>,
            alertsByStop: Map<String, AlertAssociatedStop>?
        ): List<RouteSourceData> =
            routeData.routesWithSegmentedShapes.map {
                generateRouteSource(it.routeId, it.segmentedShapes, stopsById, alertsByStop)
            }

        private fun generateRouteSource(
            routeId: String,
            routeShapes: List<SegmentedRouteShape>,
            stopsById: Map<String, Stop>,
            alertsByStop: Map<String, AlertAssociatedStop>?
        ): RouteSourceData {
            val routeLines = generateRouteLines(routeShapes, stopsById, alertsByStop)
            val features =
                routeLines.map { lineData ->
                    val feature = Feature.fromGeometry(lineData.line)
                    feature.addStringProperty(propAlertStateKey, lineData.alertState.name)
                    feature
                }
            return RouteSourceData(routeId, routeLines, getRouteSourceId(routeId), features)
        }

        private fun generateRouteLines(
            routeShapes: List<SegmentedRouteShape>,
            stopsById: Map<String, Stop>,
            alertsByStop: Map<String, AlertAssociatedStop>?
        ): List<RouteLineData> =
            routeShapes.flatMap { routeShapeToLineData(it, stopsById, alertsByStop) }

        private fun routeShapeToLineData(
            routePatternShape: SegmentedRouteShape,
            stopsById: Map<String, Stop>,
            alertsByStop: Map<String, AlertAssociatedStop>?
        ): List<RouteLineData> {
            val polyline = routePatternShape.shape.polyline ?: return emptyList()
            val fullLineString = LineString.fromPolyline(polyline, 5)
            val alertAwareSegments =
                routePatternShape.routeSegments.flatMap {
                    it.splitAlertingSegments(alertsByStop ?: emptyMap())
                }
            return alertAwareSegments.mapNotNull {
                routeSegmentToRouteLineData(it, fullLineString, stopsById)
            }
        }

        private fun routeSegmentToRouteLineData(
            segment: AlertAwareRouteSegment,
            fullLineString: LineString,
            stopsById: Map<String, Stop>
        ): RouteLineData? {
            val firstStopId = segment.stopIds.firstOrNull() ?: return null
            val firstStop = stopsById[firstStopId] ?: return null
            val lastStopId = segment.stopIds.lastOrNull() ?: return null
            val lastStop = stopsById[lastStopId] ?: return null
            if (firstStop.position == lastStop.position) return null
            val lineSegment =
                lineSlice(firstStop.position.toPoint(), lastStop.position.toPoint(), fullLineString)
            return RouteLineData(
                segment.id,
                segment.sourceRoutePatternId,
                lineSegment,
                segment.stopIds,
                segment.alertState
            )
        }
    }
}
