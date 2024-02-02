package com.mbta.tid.mbta_app

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val data: SearchResults,
)
