package com.mbta.tid.mbta_app.android.map

import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse

class RouteLayerGenerator(
    mapFriendlyRouteResponse: MapFriendlyRouteResponse,
    routesById: Map<String, Route>
) {
    val routeLayers =
        createAllRouteLayers(mapFriendlyRouteResponse.routesWithSegmentedShapes, routesById)

    companion object {
        private val routeLayerId = "route-layer"

        fun getRouteLayerId(routeId: String) = "$routeLayerId-$routeId"

        internal fun createAllRouteLayers(
            routesWithShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
            routesById: Map<String, Route>
        ): List<LineLayer> {
            return routesWithShapes
                .mapNotNull { routesById[it.routeId] }
                .sortedWith(reverseOrder())
                .flatMap { createRouteLayers(it) }
        }

        /**
         * Define the line layers for styling the route's line shapes. Returns a list of 2
         * LineLayers - one with a styling to be applied to the entirety of all shapes in the route,
         * and a second that is applied only to the portions of the lines that are alerting.
         */
        private fun createRouteLayers(route: Route): List<LineLayer> {
            fun LineLayer.routeStyle() =
                this.lineWidth(4.0)
                    .lineBorderWidth(1.0)
                    .lineBorderColor("#FFFFFF")
                    .lineJoin(LineJoin.ROUND)
                    .lineCap(LineCap.ROUND)

            val alertingLayer =
                LineLayer(
                        getRouteLayerId("${route.id}-alerting"),
                        RouteSourceGenerator.getRouteSourceId(route.id)
                    )
                    .filter(Expression.get(RouteSourceGenerator.propIsAlertingKey))
                    .routeStyle()
                    .lineDasharray(listOf(2.0, 3.0))
                    .lineColor("#FFFFFF")

            val nonAlertingLayer =
                LineLayer(
                        getRouteLayerId(route.id),
                        RouteSourceGenerator.getRouteSourceId(route.id)
                    )
                    .routeStyle()
                    .lineColor("#${route.color}")

            return listOf(nonAlertingLayer, alertingLayer)
        }
    }
}
