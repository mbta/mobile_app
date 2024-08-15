package com.mbta.tid.mbta_app.map.style

import io.github.dellisd.spatialk.geojson.Geometry

data class Feature(
    val id: String? = null,
    val geometry: Geometry,
    val properties: FeatureProperties
)
