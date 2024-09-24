package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Route(
    override val id: String,
    val type: RouteType,
    val color: String,
    @SerialName("direction_names") val directionNames: List<String?>,
    @SerialName("direction_destinations") val directionDestinations: List<String?>,
    @SerialName("long_name") val longName: String,
    @SerialName("short_name") val shortName: String,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("text_color") val textColor: String,
    @SerialName("line_id") val lineId: String? = null,
    @SerialName("route_pattern_ids") val routePatternIds: List<String>? = null
) : Comparable<Route>, BackendObject {
    override fun compareTo(other: Route) = sortOrder.compareTo(other.sortOrder)

    val label: String =
        when (type) {
            RouteType.BUS -> shortName
            RouteType.COMMUTER_RAIL -> longName.replace("/", " / ")
            else -> longName
        }

    companion object {
        val subwayFirstComparator =
            Comparator<Route> { route1, route2 ->
                if (route1.type.isSubway() && !route2.type.isSubway()) {
                    -1
                } else if (route2.type.isSubway() && !route1.type.isSubway()) {
                    1
                } else {
                    0
                }
            }

        fun pinnedRoutesComparator(pinnedRoutes: Set<String>): Comparator<Route> = compareBy {
            !pinnedRoutes.contains(it.id) && !pinnedRoutes.contains(it.lineId)
        }

        /*
        Sort by pinned status first, then subway first, then given sort order.
         */
        fun relevanceComparator(pinnedRoutes: Set<String>): Comparator<Route> =
            pinnedRoutesComparator(pinnedRoutes).then(subwayFirstComparator)
    }
}
