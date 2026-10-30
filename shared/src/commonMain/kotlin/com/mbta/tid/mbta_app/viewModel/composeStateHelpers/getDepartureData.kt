package com.mbta.tid.mbta_app.viewModel.composeStateHelpers

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.ErrorKey
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.compose.koinInject

/**
 * Schedules and predictions loaded for a set of stops. Predictions are only meaningful alongside
 * schedules (schedules determine what to show when there are no predictions, and whether a
 * predicted trip is the last one), so use [matchingResponses] to get data that is safe to display.
 */
internal data class DepartureData(
    val schedules: LoadedSchedules?,
    val predictions: LoadedPredictions?,
) {
    internal data class DepartureResponses(
        val schedules: ScheduleResponse,
        val predictions: PredictionsStreamDataResponse,
    )

    /**
     * Returns the loaded responses only if both were loaded for exactly [stopIds], and the
     * schedules are for the service date of [now]. Otherwise, returns null.
     */
    fun matchingResponses(stopIds: Set<String>, now: EasternTimeInstant): DepartureResponses? {
        val schedules = schedules ?: return null
        val predictions = predictions ?: return null
        if (schedules.stopIds != stopIds || predictions.stopIds != stopIds) return null
        if (schedules.serviceDate != now.serviceDate) return null
        return DepartureResponses(schedules.response, predictions.response)
    }
}

/**
 * Loads schedules and subscribes to predictions for [stopIds], for use in view models that need to
 * display both together.
 */
@Composable
internal fun getDepartureData(
    stopIds: Set<String>?,
    now: EasternTimeInstant,
    sheetRoute: SheetRoutes?,
    active: Boolean,
    errorKey: ErrorKey,
    onAnyMessageReceived: () -> Unit = {},
    errorBannerRepository: IErrorBannerStateRepository = koinInject(),
    schedulesRepository: ISchedulesRepository = koinInject(),
    predictionsRepository: IPredictionsRepository = koinInject(),
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
): DepartureData {
    val schedules =
        getSchedules(
            stopIds,
            now,
            active,
            errorKey,
            schedulesRepository,
            errorBannerRepository,
            coroutineDispatcher,
        )
    val predictions =
        subscribeToPredictions(
            stopIds,
            sheetRoute,
            active,
            errorKey,
            onAnyMessageReceived = onAnyMessageReceived,
            errorBannerRepository = errorBannerRepository,
            predictionsRepository = predictionsRepository,
        )
    return DepartureData(schedules, predictions)
}
