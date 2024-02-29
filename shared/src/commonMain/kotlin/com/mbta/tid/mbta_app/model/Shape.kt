package com.mbta.tid.mbta_app.model

import kotlinx.serialization.Serializable

@Serializable data class Shape(val id: String, val polyline: String? = null)
