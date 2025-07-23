package com.mbta.tid.mbta_app.viewModel.composeStateHelpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private fun fetchSchedules(
    stopIds: List<String>,
    errorKey: String,
    errorBannerRepository: IErrorBannerStateRepository,
    schedulesRepository: ISchedulesRepository,
    onSuccess: (ScheduleResponse) -> Unit,
) {
    CoroutineScope(Dispatchers.IO).launch {
        fetchApi(
            errorBannerRepo = errorBannerRepository,
            errorKey = errorKey,
            getData = { schedulesRepository.getSchedule(stopIds, Clock.System.now()) },
            onSuccess = onSuccess,
            onRefreshAfterError = {
                fetchSchedules(
                    stopIds,
                    errorKey,
                    errorBannerRepository,
                    schedulesRepository,
                    onSuccess,
                )
            },
        )
    }
}

@Composable
fun getSchedules(
    stopIds: List<String>?,
    errorKey: String,
    schedulesRepository: ISchedulesRepository = koinInject(),
    errorBannerRepository: IErrorBannerStateRepository = koinInject(),
): ScheduleResponse? {
    var result: ScheduleResponse? by remember { mutableStateOf(null) }

    LaunchedEffect(stopIds) {
        if (stopIds != null) {
            fetchSchedules(stopIds, errorKey, errorBannerRepository, schedulesRepository) {
                result = it
            }
        } else {
            result = ScheduleResponse(emptyList(), emptyMap())
        }
    }

    return result
}
