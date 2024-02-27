package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.GetReferenceIdSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoutePattern(
    val id: String,
    @SerialName("direction_id") val directionId: Int,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int,
    val typicality: Typicality?,
    @SerialName("representative_trip") val representativeTrip: Trip? = null,
    @Serializable(with = GetReferenceIdSerializer::class) @SerialName("route") val routeId: String
) : Comparable<RoutePattern> {
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
