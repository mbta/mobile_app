package com.mbta.tid.mbta_app.model

import kotlinx.serialization.Serializable

@Serializable
public data class SearchResults(
    internal val routes: List<RouteResult> = emptyList(),
    internal val stops: List<StopResult> = emptyList(),
)
