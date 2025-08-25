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
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.coroutines.CoroutineDispatcher
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
    coroutineDispatcher: CoroutineDispatcher,
    onSuccess: (ScheduleResponse) -> Unit,
) {
    CoroutineScope(coroutineDispatcher).launch {
        fetchApi(
            errorBannerRepo = errorBannerRepository,
            errorKey = errorKey,
            getData = { schedulesRepository.getSchedule(stopIds, EasternTimeInstant.now()) },
            onSuccess = onSuccess,
            onRefreshAfterError = {
                fetchSchedules(
                    stopIds,
                    errorKey,
                    errorBannerRepository,
                    schedulesRepository,
                    coroutineDispatcher,
                    onSuccess,
                )
            },
        )
    }
}

@Composable
internal fun getSchedules(
    stopIds: List<String>?,
    errorKey: String,
    schedulesRepository: ISchedulesRepository = koinInject(),
    errorBannerRepository: IErrorBannerStateRepository = koinInject(),
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
): ScheduleResponse? {
    val errorKey = "$errorKey.getSchedules"
    var result: ScheduleResponse? by remember { mutableStateOf(null) }

    LaunchedEffect(stopIds) {
        if (stopIds != null) {
            fetchSchedules(
                stopIds,
                errorKey,
                errorBannerRepository,
                schedulesRepository,
                coroutineDispatcher,
            ) {
                result = it
            }
        } else {
            result = ScheduleResponse(emptyList(), emptyMap())
        }
    }

    return result
}
