package com.mbta.tid.mbta_app.kdTree

import io.github.dellisd.spatialk.geojson.Position

internal enum class Axis {
    Longitude,
    Latitude;

    fun next(): Axis =
        when (this) {
            Longitude -> Latitude
            Latitude -> Longitude
        }
}

internal operator fun Position.get(axis: Axis): Double =
    when (axis) {
        Axis.Longitude -> longitude
        Axis.Latitude -> latitude
    }

internal fun Position.butWith(axis: Axis, value: Double) =
    when (axis) {
        Axis.Longitude -> Position(longitude = value, latitude = latitude)
        Axis.Latitude -> Position(longitude = longitude, latitude = value)
    }
