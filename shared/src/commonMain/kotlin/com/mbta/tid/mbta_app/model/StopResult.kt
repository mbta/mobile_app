package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class StopResult(
    val id: String,
    val rank: Int,
    val name: String,
    val zone: String?,
    @SerialName("station?") val isStation: Boolean,
    internal val routes: List<StopResultRoute>,
)

@Serializable
public data class StopResultRoute(internal val type: RouteType, internal val icon: String)
