package com.mbta.tid.mbta_app.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RouteStopsResponse(@SerialName("stop_ids") val stopIds: List<String>)
