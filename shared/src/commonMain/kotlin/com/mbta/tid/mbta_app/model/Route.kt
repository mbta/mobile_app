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

        /*
        Sort by pinned status first, then subway first, defaulting to route sort order.
         */
        fun relevanceComparator(pinnedRoutes: Set<String>): Comparator<Route> {
            return compareBy<Route> { !pinnedRoutes.contains(it.id) }
                .then(subwayFirstComparator)
                .thenBy { it.sortOrder }
        }
    }
}
