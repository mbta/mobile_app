package com.mbta.tid.mbta_app.viewModel.composeStateHelpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.ITripPredictionsRepository
import com.mbta.tid.mbta_app.utils.timer
import com.mbta.tid.mbta_app.viewModel.TripDetailsViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import org.koin.compose.koinInject

internal fun tripPredictionsErrorKey(prefix: String) = "$prefix.subscribeToTripPredictions"

@OptIn(ExperimentalTime::class)
@Composable
internal fun subscribeToTripPredictions(
    tripId: String?,
    errorKey: String,
    active: Boolean,
    context: TripDetailsViewModel.Context?,
    onAnyMessageReceived: () -> Unit = {},
    errorBannerRepository: IErrorBannerStateRepository = koinInject(),
    tripPredictionsRepository: ITripPredictionsRepository = koinInject(),
    checkPredictionsStaleInterval: Duration = 5.seconds,
): PredictionsStreamDataResponse? {
    val errorKey = tripPredictionsErrorKey(errorKey)
    val staleTimer by timer(checkPredictionsStaleInterval)

    var predictions: PredictionsStreamDataResponse? by remember { mutableStateOf(null) }

    fun connect(
        tripId: String?,
        active: Boolean,
        onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit,
    ) {
        tripPredictionsRepository.disconnect()
        if (tripId != null && active) {
            tripPredictionsRepository.connect(tripId, onReceive)
        }
    }

    fun onReceive(message: ApiResult<PredictionsStreamDataResponse>) {
        onAnyMessageReceived()
        when (message) {
            is ApiResult.Ok -> {
                errorBannerRepository.clearDataError(errorKey)
                predictions = message.data
            }
            is ApiResult.Error -> {
                errorBannerRepository.setDataError(errorKey, message.toString()) {
                    connect(tripId, active, ::onReceive)
                }
                println("Trip predictions stream failed to join: ${message.message}")
            }
        }
    }

    fun checkStale() {
        // Skip stale checks when the context is not trip details, because there are already stale
        // checks on stop details, and they'll clash if both are happening simultaneously
        if (context != TripDetailsViewModel.Context.TripDetails || !active) return
        val lastUpdated = tripPredictionsRepository.lastUpdated
        if (lastUpdated != null) {
            errorBannerRepository.checkPredictionsStale(
                predictionsLastUpdated = lastUpdated,
                predictionQuantity = predictions?.predictionQuantity() ?: 0,
                action = { connect(tripId, active, ::onReceive) },
            )
        }
    }

    LaunchedEffect(tripId) { predictions = null }
    DisposableEffect(tripId, active) {
        connect(tripId, active, ::onReceive)
        onDispose { tripPredictionsRepository.disconnect() }
    }

    LaunchedEffect(predictions) { checkStale() }
    LaunchedEffect(staleTimer) { checkStale() }

    LaunchedEffect(active) {
        if (
            active &&
                predictions != null &&
                tripPredictionsRepository.shouldForgetPredictions(
                    predictions?.predictionQuantity() ?: 0
                )
        ) {
            predictions = null
        }
    }

    return predictions
}
