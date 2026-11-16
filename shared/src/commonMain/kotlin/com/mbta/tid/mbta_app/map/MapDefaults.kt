package com.mbta.tid.mbta_app.map

import org.maplibre.spatialk.geojson.Position

public object MapDefaults {
    public val midZoomThreshold: Double = 11.0
    internal val closeZoomThreshold: Double = 15.0
    public val zoom: Double = 16.0
    public val center: Position = Position(longitude = -71.0601, latitude = 42.3537)
}
