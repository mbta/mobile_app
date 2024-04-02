package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NearbyResponse(@SerialName("stop_ids") val stopIds: List<String>) {
    constructor(objects: ObjectCollectionBuilder) : this(objects.stops.keys.toList())
}
