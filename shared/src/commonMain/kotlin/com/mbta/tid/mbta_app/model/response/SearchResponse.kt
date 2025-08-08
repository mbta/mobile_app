package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.SearchResults
import kotlinx.serialization.Serializable

@Serializable
internal data class SearchResponse(val data: SearchResults) {
    override fun toString() = "[SearchResponse]"
}
