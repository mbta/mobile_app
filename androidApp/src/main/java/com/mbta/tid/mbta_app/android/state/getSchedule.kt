package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mbta.tid.mbta_app.android.util.fetchApi
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.compose.koinInject

class ScheduleViewModel(
    private val schedulesRepository: ISchedulesRepository,
    private val errorBannerRepository: IErrorBannerStateRepository
) : ViewModel() {
    private val _schedule = MutableStateFlow<ScheduleResponse?>(null)
    val schedule: StateFlow<ScheduleResponse?> = _schedule

    fun getSchedule(stopIds: List<String>, errorKey: String) {
        if (stopIds.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                fetchApi(
                    errorBannerRepo = errorBannerRepository,
                    errorKey = errorKey,
                    getData = { schedulesRepository.getSchedule(stopIds, Clock.System.now()) },
                    onSuccess = { _schedule.value = it },
                    onRefreshAfterError = { getSchedule(stopIds, errorKey) }
                )
            }
        } else {
            _schedule.value = ScheduleResponse(emptyList(), emptyMap())
        }
    }

    class Factory(
        private val schedulesRepository: ISchedulesRepository,
        private val errorBannerRepository: IErrorBannerStateRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScheduleViewModel(schedulesRepository, errorBannerRepository) as T
        }
    }
}

@Composable
fun getSchedule(
    stopIds: List<String>?,
    errorKey: String,
    schedulesRepository: ISchedulesRepository = koinInject(),
    errorBannerRepository: IErrorBannerStateRepository = koinInject(),
): ScheduleResponse? {
    var viewModel: ScheduleViewModel =
        viewModel(factory = ScheduleViewModel.Factory(schedulesRepository, errorBannerRepository))

    LaunchedEffect(key1 = stopIds) { viewModel.getSchedule(stopIds ?: emptyList(), errorKey) }

    return viewModel?.schedule?.collectAsState(initial = null)?.value
}
