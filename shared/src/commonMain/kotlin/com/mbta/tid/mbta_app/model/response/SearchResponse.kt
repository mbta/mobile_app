package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.SearchResults
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(val data: SearchResults) {
    override fun toString() = "[SearchResponse]"
}
