package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.android.util.fetchApi
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
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
    private val schedulesRepository: ISchedulesRepository,
    private val errorBannerRepository: IErrorBannerStateRepository
) : ViewModel() {
    private val _schedule = MutableStateFlow<ScheduleResponse?>(null)
    val schedule: StateFlow<ScheduleResponse?> = _schedule

    init {
        CoroutineScope(Dispatchers.IO).launch { schedule.collect { getSchedule(stopIds) } }
    }

    private fun getSchedule(stopIds: List<String>) {
        if (stopIds.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                fetchApi(
                    errorBannerRepo = errorBannerRepository,
                    errorKey = "ScheduleViewModel.getSchedule",
                    getData = { schedulesRepository.getSchedule(stopIds, Clock.System.now()) },
                    onSuccess = { _schedule.emit(it) },
                    onRefreshAfterError = { getSchedule(stopIds) }
                )
            }
        } else {
            _schedule.value = ScheduleResponse(emptyList(), emptyMap())
        }
    }
}

@Composable
fun getSchedule(
    stopIds: List<String>?,
    schedulesRepository: ISchedulesRepository = koinInject(),
    errorBannerRepository: IErrorBannerStateRepository = koinInject()
): ScheduleResponse? {
    var viewModel: ScheduleViewModel? =
        remember(stopIds) {
            ScheduleViewModel(stopIds ?: emptyList(), schedulesRepository, errorBannerRepository)
        }

    return viewModel?.schedule?.collectAsState(initial = null)?.value
}
