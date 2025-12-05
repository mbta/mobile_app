package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Schedule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class NextScheduleResponse(@SerialName("next_schedule") val nextSchedule: Schedule?)
