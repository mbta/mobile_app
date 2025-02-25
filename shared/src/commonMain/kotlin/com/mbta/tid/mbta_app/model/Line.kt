package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Line(
    override val id: String,
    val color: String,
    @SerialName("long_name") val longName: String,
    @SerialName("short_name") val shortName: String,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("text_color") val textColor: String,
) : BackendObject {
    /** Grouped lines are displayed as though they are different branches of a single route. */
    val isGrouped = this.id in groupedIds

    companion object {
        val groupedIds = setOf("line-Green")
    }
}
