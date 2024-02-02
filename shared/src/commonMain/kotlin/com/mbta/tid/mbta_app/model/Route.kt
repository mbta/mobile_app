package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Route(
    val id: String,
    val color: String,
    @SerialName("direction_names") val directionNames: List<String>,
    @SerialName("direction_destinations") val directionDestinations: List<String>,
    @SerialName("long_name") val longName: String,
    @SerialName("short_name") val shortName: String,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("text_color") val textColor: String
)
