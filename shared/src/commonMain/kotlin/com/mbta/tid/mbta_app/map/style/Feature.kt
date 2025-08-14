package com.mbta.tid.mbta_app.map.style

import io.github.dellisd.spatialk.geojson.Geometry

public data class Feature
internal constructor(
    val id: String? = null,
    val geometry: Geometry,
    val properties: FeatureProperties,
)
