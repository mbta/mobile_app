package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.compose.koinInject

class ScheduleViewModel(
    stopIds: List<String>,
    private val schedulesRepository: ISchedulesRepository
) : ViewModel() {
    private val _schedule = MutableStateFlow<ScheduleResponse?>(null)
    val schedule: StateFlow<ScheduleResponse?> = _schedule

    init {
        CoroutineScope(Dispatchers.IO).launch { schedule.collect { getSchedule(stopIds) } }
    }

    private suspend fun getSchedule(stopIds: List<String>) {
        if (stopIds.size > 0) {
            when (val data = schedulesRepository.getSchedule(stopIds, Clock.System.now())) {
                is ApiResult.Ok -> _schedule.value = data.data
                is ApiResult.Error -> {
                    /* TODO("handle errors") */
                }
            }
        } else {
            _schedule.value = ScheduleResponse(emptyList(), emptyMap())
        }
    }
}

@Composable
fun getSchedule(
    stopIds: List<String>?,
    schedulesRepository: ISchedulesRepository = koinInject()
): ScheduleResponse? {
    var viewModel: ScheduleViewModel? =
        remember(stopIds) { ScheduleViewModel(stopIds ?: emptyList(), schedulesRepository) }

    return viewModel?.schedule?.collectAsState(initial = null)?.value
}
