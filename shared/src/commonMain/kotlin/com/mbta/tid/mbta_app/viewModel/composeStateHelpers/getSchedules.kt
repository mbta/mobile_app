package com.mbta.tid.mbta_app.viewModel.composeStateHelpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.ErrorKey
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject

/**
 * Schedules loaded for [stopIds] on [serviceDate]. Schedules are only valid for the service date
 * they were fetched for, so consumers must check both before using [response].
 */
internal data class LoadedSchedules(
    val stopIds: Set<String>,
    val serviceDate: LocalDate,
    val response: ScheduleResponse,
)

/**
 * Loads the schedules for [stopIds] at [now]. The result is cleared and reloaded whenever [stopIds]
 * or the service date of [now] changes, and refreshed whenever [active] changes or a retry is
 * requested from the error banner. Requests that are superseded are canceled, so a late response
 * can never replace the result of a newer request.
 */
@Composable
internal fun getSchedules(
    stopIds: Set<String>?,
    now: EasternTimeInstant,
    active: Boolean,
    errorKey: ErrorKey,
    schedulesRepository: ISchedulesRepository = koinInject(),
    errorBannerRepository: IErrorBannerStateRepository = koinInject(),
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
): LoadedSchedules? {
    val errorKey = errorKey.withSuffix("getSchedules")
    var result: LoadedSchedules? by remember { mutableStateOf(null) }
    var retryCount: Int by remember { mutableStateOf(0) }
    val serviceDate = now.serviceDate

    LaunchedEffect(stopIds, serviceDate, active, retryCount) {
        val current = result
        val currentIsRelevant =
            stopIds != null && current?.stopIds == stopIds && current.serviceDate == serviceDate
        if (!currentIsRelevant) result = null
        if (stopIds == null) return@LaunchedEffect

        withContext(coroutineDispatcher) {
            fetchApi(
                errorBannerRepo = errorBannerRepository,
                errorKey = errorKey,
                getData = { schedulesRepository.getSchedule(stopIds, now) },
                onSuccess = { result = LoadedSchedules(stopIds, serviceDate, it) },
                onRefreshAfterError = { retryCount++ },
            )
        }
    }

    return result
}
