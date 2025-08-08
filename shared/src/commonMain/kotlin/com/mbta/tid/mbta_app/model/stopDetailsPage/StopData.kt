package com.mbta.tid.mbta_app.model.stopDetailsPage

import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlinx.serialization.Serializable

@Serializable
public data class StopData(
    val stopId: String,
    val schedules: ScheduleResponse?,
    val predictionsByStop: PredictionsByStopJoinResponse?,
    val predictionsLoaded: Boolean = false,
)
