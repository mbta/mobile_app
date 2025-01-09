package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mbta.tid.mbta_app.android.util.fetchApi
import com.mbta.tid.mbta_app.model.response.ApiResult
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

    fun getSchedule(stopIds: List<String>, context: String) {
        if (stopIds.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                if (stopIds == listOf("place-pktrm")) {
                    //     Log.i("KB", "fetching for pktrm")
                    fetchApi(
                        errorBannerRepo = errorBannerRepository,
                        errorKey = "ScheduleViewModel.getSchedule ${context}",
                        getData = {
                            //     Log.i("KB", "fetch for park")
                            ApiResult.Error<ScheduleResponse>(500, "oops")
                        },
                        onSuccess = { _schedule.value = it },
                        onRefreshAfterError = { getSchedule(stopIds, context) }
                    )
                } else {
                    //       Log.i("KB", "fetching for ${stopIds}")

                    fetchApi(
                        errorBannerRepo = errorBannerRepository,
                        errorKey = "ScheduleViewModel.getSchedule",
                        getData = {
                            //      Log.i("KB", "fetch for stops ${stopIds}")
                            schedulesRepository.getSchedule(stopIds, Clock.System.now())
                        },
                        onSuccess = { _schedule.value = it },
                        onRefreshAfterError = { getSchedule(stopIds, context) }
                    )
                }
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
    schedulesRepository: ISchedulesRepository = koinInject(),
    errorBannerRepository: IErrorBannerStateRepository = koinInject(),
    context: String = ""
): ScheduleResponse? {
    //  Log.i("KB","Stop ids ${stopIds}")
    var viewModel: ScheduleViewModel =
        viewModel(factory = ScheduleViewModel.Factory(schedulesRepository, errorBannerRepository))

    LaunchedEffect(key1 = stopIds) { viewModel.getSchedule(stopIds ?: emptyList(), context) }

    return viewModel?.schedule?.collectAsState(initial = null)?.value
}
