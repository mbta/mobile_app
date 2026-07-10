package com.mbta.tid.mbta_app.utils

import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.turf.coordinatemutation.round

public fun Position.isRoughlyEqualTo(other: Position): Boolean =
    this.latitude.round(6) == other.latitude.round(6) &&
        this.longitude.round(6) == other.longitude.round(6)
