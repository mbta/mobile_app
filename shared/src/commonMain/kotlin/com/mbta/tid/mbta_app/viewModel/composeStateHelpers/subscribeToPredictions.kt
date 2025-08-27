package com.mbta.tid.mbta_app.viewModel.composeStateHelpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.orEmpty
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.utils.timer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.koin.compose.koinInject

@Composable
internal fun subscribeToPredictions(
    stopIds: List<String>?,
    active: Boolean,
    errorKey: String,
    onAnyMessageReceived: () -> Unit = {},
    errorBannerRepository: IErrorBannerStateRepository = koinInject(),
    predictionsRepository: IPredictionsRepository = koinInject(),
    checkPredictionsStaleInterval: Duration = 5.seconds,
): PredictionsStreamDataResponse? {
    val errorKey = "$errorKey.subscribeToPredictions"
    val staleTimer by timer(checkPredictionsStaleInterval)

    var predictions: PredictionsByStopJoinResponse? by remember { mutableStateOf(null) }
    var loadedStopIds: List<String>? by remember { mutableStateOf(null) }

    fun connect(
        stopIds: List<String>?,
        active: Boolean,
        onJoin: (ApiResult<PredictionsByStopJoinResponse>) -> Unit,
        onMessage: (ApiResult<PredictionsByStopMessageResponse>) -> Unit,
    ) {
        predictionsRepository.disconnect()
        if (stopIds != null && active) {
            predictionsRepository.connectV2(stopIds, onJoin, onMessage)
        }
    }

    fun onMessage(message: ApiResult<PredictionsByStopMessageResponse>) {
        onAnyMessageReceived()
        when (message) {
            is ApiResult.Ok -> {
                errorBannerRepository.clearDataError(errorKey)
                predictions = predictions.orEmpty().mergePredictions(message.data)
            }
            is ApiResult.Error ->
                println("Predictions stream failed on message: ${message.message}")
        }
    }

    fun onJoin(message: ApiResult<PredictionsByStopJoinResponse>) {
        onAnyMessageReceived()
        when (message) {
            is ApiResult.Ok -> {
                errorBannerRepository.clearDataError(errorKey)
                loadedStopIds = stopIds
                predictions = message.data
            }
            is ApiResult.Error -> {
                errorBannerRepository.setDataError(errorKey, message.toString()) {
                    connect(stopIds, active, ::onJoin, ::onMessage)
                }
                println("Predictions stream failed to join: ${message.message}")
            }
        }
    }

    fun checkStale() {
        val lastUpdated = predictionsRepository.lastUpdated
        if (lastUpdated != null) {
            errorBannerRepository.checkPredictionsStale(
                predictionsLastUpdated = lastUpdated,
                predictionQuantity = predictions?.predictionQuantity() ?: 0,
                action = { connect(stopIds, active, ::onJoin, ::onMessage) },
            )
        }
    }

    DisposableEffect(stopIds, active) {
        if (loadedStopIds != stopIds) {
            predictions = null
            loadedStopIds = null
        }
        connect(stopIds, active, ::onJoin, ::onMessage)
        onDispose { predictionsRepository.disconnect() }
    }

    LaunchedEffect(predictions) { checkStale() }
    LaunchedEffect(staleTimer) { checkStale() }

    // when becoming active, if predictions should be forgotten, reset predictions
    LaunchedEffect(active) {
        if (
            active &&
                predictions != null &&
                predictionsRepository.shouldForgetPredictions(
                    predictions?.predictionQuantity() ?: 0
                )
        ) {
            predictions = null
        }
    }

    return predictions?.toPredictionsStreamDataResponse()
}
