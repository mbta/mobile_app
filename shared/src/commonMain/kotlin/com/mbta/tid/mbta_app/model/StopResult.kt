package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StopResult(
    val id: String,
    val rank: Int,
    val name: String,
    val zone: String?,
    @SerialName("station?") val isStation: Boolean,
    val routes: List<StopResultRoute>,
)

@Serializable data class StopResultRoute(val type: RouteType, val icon: String)
