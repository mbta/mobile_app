package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class RouteResult
internal constructor(
    val id: String,
    val rank: Int,
    @SerialName("long_name") val longName: String,
    @SerialName("name") val shortName: String,
    @SerialName("route_type") val routeType: RouteType,
) {
    /** Convenience constructor for testing */
    public constructor(
        route: Route,
        rank: Int = 1,
    ) : this(
        id = route.id,
        rank = rank,
        longName = route.longName,
        shortName = route.shortName,
        routeType = route.type,
    )
}
