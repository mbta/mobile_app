package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoutePattern(
    override val id: String,
    @SerialName("direction_id") val directionId: Int,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int,
    val typicality: Typicality?,
    @SerialName("representative_trip_id") val representativeTripId: String,
    @SerialName("route_id") val routeId: String
) : Comparable<RoutePattern>, BackendObject {
    @Serializable
    enum class Typicality {
        @SerialName("typical") Typical,
        @SerialName("deviation") Deviation,
        @SerialName("atypical") Atypical,
        @SerialName("diversion") Diversion,
        @SerialName("canonical_only") CanonicalOnly
    }

    override fun compareTo(other: RoutePattern) = sortOrder.compareTo(other.sortOrder)
}
