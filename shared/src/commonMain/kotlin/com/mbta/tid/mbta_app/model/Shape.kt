package com.mbta.tid.mbta_app.model

import kotlinx.serialization.Serializable

@Serializable
public data class Shape
internal constructor(override val id: String, val polyline: String? = null) : BackendObject<String>
