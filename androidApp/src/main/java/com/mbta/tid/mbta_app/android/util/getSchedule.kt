package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import org.koin.compose.koinInject

@Composable
fun getSchedule(
    stopIds: List<String>?,
    now: Instant,
    schedulesRepository: ISchedulesRepository = koinInject()
): ScheduleResponse? {
    var schedules: ScheduleResponse? by remember { mutableStateOf(null) }

    LaunchedEffect(stopIds, now) {
        if (stopIds != null) {
            withContext(Dispatchers.IO) {
                schedules = schedulesRepository.getSchedule(stopIds, now)
            }
        }
    }

    return schedules
}
