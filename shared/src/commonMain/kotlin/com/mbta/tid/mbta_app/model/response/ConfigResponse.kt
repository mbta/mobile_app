package com.mbta.tid.mbta_app.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigResponse(@SerialName("mapbox_public_token") val mapboxPublicToken: String)
