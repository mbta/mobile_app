package com.mbta.tid.mbta_app.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchResults(
    val routes: List<RouteResult>,
    val stops: List<StopResult>,
)
