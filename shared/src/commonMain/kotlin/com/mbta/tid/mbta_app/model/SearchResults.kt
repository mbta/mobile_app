package com.mbta.tid.mbta_app.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchResults(
    val routes: List<RouteResult> = emptyList(),
    val stops: List<StopResult> = emptyList(),
)
