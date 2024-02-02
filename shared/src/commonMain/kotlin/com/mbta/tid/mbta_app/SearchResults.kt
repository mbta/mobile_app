package com.mbta.tid.mbta_app

import kotlinx.serialization.Serializable

@Serializable
data class SearchResults(
    val routes: List<RouteResult>,
    val stops: List<StopResult>,
)
