package com.mbta.tid.mbta_app.map.style

import org.maplibre.spatialk.geojson.Geometry

public data class Feature
internal constructor(
    val id: String? = null,
    val geometry: Geometry,
    val properties: FeatureProperties,
)
