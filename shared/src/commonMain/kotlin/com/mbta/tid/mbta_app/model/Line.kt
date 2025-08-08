package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class Line(
    override val id: String,
    val color: String,
    @SerialName("long_name") val longName: String,
    @SerialName("short_name") internal val shortName: String,
    @SerialName("sort_order") internal val sortOrder: Int,
    @SerialName("text_color") val textColor: String,
) : BackendObject {
    /** Grouped lines are displayed as though they are different branches of a single route. */
    internal val isGrouped = this.id in groupedIds

    internal companion object {
        val groupedIds = setOf("line-Green")
    }
}
