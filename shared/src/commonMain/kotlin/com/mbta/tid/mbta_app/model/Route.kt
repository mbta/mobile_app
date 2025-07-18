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
    @SerialName("listed_route") val isListedRoute: Boolean,
    @SerialName("long_name") val longName: String,
    @SerialName("short_name") val shortName: String,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("text_color") val textColor: String,
    @SerialName("line_id") val lineId: String? = null,
    @SerialName("route_pattern_ids") val routePatternIds: List<String>? = null,
) : Comparable<Route>, BackendObject {
    override fun compareTo(other: Route) = sortOrder.compareTo(other.sortOrder)

    val label: String =
        when (type) {
            RouteType.BUS -> shortName
            RouteType.COMMUTER_RAIL -> longName.replace("/", " / ")
            else -> longName
        }

    val isShuttle: Boolean = id.startsWith("Shuttle")
}
