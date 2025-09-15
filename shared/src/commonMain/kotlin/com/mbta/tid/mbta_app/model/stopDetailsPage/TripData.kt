package com.mbta.tid.mbta_app.model.stopDetailsPage

import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import kotlinx.serialization.Serializable

@Serializable
public data class TripData(
    val tripFilter: TripDetailsPageFilter,
    val trip: Trip,
    val tripSchedules: TripSchedulesResponse?,
    val tripPredictions: PredictionsStreamDataResponse?,
    val tripPredictionsLoaded: Boolean = false,
    val vehicle: Vehicle?,
)
