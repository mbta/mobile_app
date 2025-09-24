package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.map.style.Color
import com.mbta.tid.mbta_app.model.response.GlobalResponse

// These are used to disambiguate from LineOrRoute.Route and LineOrRoute.Line

private typealias LineModel = Line

private typealias RouteModel = Route

public sealed class LineOrRoute {

    public data class Line(val line: LineModel, val routes: Set<RouteModel>) : LineOrRoute()

    public data class Route(val route: RouteModel) : LineOrRoute()

    public val id: String
        get() =
            when (this) {
                is Line -> this.line.id
                is Route -> this.route.id
            }

    public val name: String
        get() =
            when (this) {
                is Line -> this.line.longName
                is Route -> this.route.label
            }

    public val type: RouteType
        get() =
            when (this) {
                is Line -> this.sortRoute.type
                is Route -> this.route.type
            }

    public val backgroundColor: Color
        get() =
            when (this) {
                is Line -> this.line.color
                is Route -> this.route.color
            }

    public val textColor: Color
        get() =
            when (this) {
                is Line -> this.line.textColor
                is Route -> this.route.textColor
            }

    internal val isSubway: Boolean
        get() =
            when (this) {
                is Line -> this.routes.any { it.type.isSubway() }
                is Route -> this.route.type.isSubway()
            }

    /** The route whose sortOrder to use when sorting a RouteCardData. */
    public val sortRoute: RouteModel
        get() =
            when (this) {
                is Route -> this.route
                is Line -> this.routes.min()
            }

    public val allRoutes: Set<RouteModel>
        get() =
            when (this) {
                is Route -> setOf(this.route)
                is Line -> this.routes
            }

    public fun directions(
        globalData: GlobalResponse,
        stop: Stop,
        patterns: List<RoutePattern>,
    ): List<Direction> =
        when (this) {
            is Line -> Direction.getDirectionsForLine(globalData, stop, patterns)
            is Route -> Direction.getDirections(globalData, stop, this.route, patterns)
        }

    public fun containsRoute(routeId: String?): Boolean =
        when (this) {
            is Line -> this.routes.any { it.id == routeId }
            is Route -> this.id == routeId
        }
}
