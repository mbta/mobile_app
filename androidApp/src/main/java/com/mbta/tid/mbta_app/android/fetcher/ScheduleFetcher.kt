package com.mbta.tid.mbta_app.android.fetcher

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.Backend
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlinx.datetime.Instant

@Composable
fun getSchedules(backend: Backend, stopIds: List<String>?, now: Instant): ScheduleResponse? =
    // Strictly speaking, we would want to rerun this if the current service date changes, but we do
    // not want to rerun it every five seconds, so skip now in the effectKey
    getBackendData(backend, effectKey = stopIds) {
        if (stopIds != null) {
            getSchedule(stopIds, now)
        } else null
    }
