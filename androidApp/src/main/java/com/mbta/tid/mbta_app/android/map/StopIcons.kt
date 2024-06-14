package com.mbta.tid.mbta_app.android.map

import com.mapbox.maps.extension.style.expressions.dsl.generated.image
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.step
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.LocationType

enum class StopIcons(val drawableId: Int) {
    Station(R.drawable.t_station),
    StationIssues(R.drawable.t_station_issues),
    StationNoService(R.drawable.t_station_no_service),
    Stop(R.drawable.bus_stop),
    StopIssues(R.drawable.bus_stop_issues),
    StopNoService(R.drawable.bus_stop_no_service),
    StopSmall(R.drawable.bus_stop_small);

    companion object {
        fun getStopLayerIcon(locationType: LocationType): Expression =
            when (locationType) {
                LocationType.STATION -> image { literal(Station.name) }
                LocationType.STOP ->
                    step {
                        zoom()
                        stop {
                            image { literal(StopSmall.name) }
                            literal(tombstoneZoomThreshold)
                        }
                        image { literal(Stop.name) }
                    }
                else -> Expression.literal("")
            }

        val stopZoomThreshold = 13.0
        val tombstoneZoomThreshold = 16.0
    }
}
