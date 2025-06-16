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
import org.koin.compose.koinInject

@Composable
fun subscribeToPredictions(
    stopIds: List<String>?,
    active: Boolean,
    errorBannerRepository: IErrorBannerStateRepository = koinInject(),
    predictionsRepository: IPredictionsRepository = koinInject(),
): PredictionsStreamDataResponse? {
    var predictions: PredictionsByStopJoinResponse? by remember { mutableStateOf(null) }

    fun onJoin(message: ApiResult<PredictionsByStopJoinResponse>) {
        when (message) {
            is ApiResult.Ok -> predictions = message.data
            is ApiResult.Error -> println("Predictions stream failed to join: ${message.message}")
        }
    }

    fun onMessage(message: ApiResult<PredictionsByStopMessageResponse>) {
        when (message) {
            is ApiResult.Ok -> predictions = predictions.orEmpty().mergePredictions(message.data)
            is ApiResult.Error ->
                println("Predictions stream failed on message: ${message.message}")
        }
    }

    DisposableEffect(stopIds, active) {
        if (stopIds != null && active) {
            predictionsRepository.connectV2(stopIds, onJoin = ::onJoin, onMessage = ::onMessage)
        }
        onDispose { predictionsRepository.disconnect() }
    }

    LaunchedEffect(predictions) {
        val lastUpdated = predictionsRepository.lastUpdated
        if (lastUpdated != null) {
            errorBannerRepository.checkPredictionsStale(
                predictionsLastUpdated = lastUpdated,
                predictionQuantity = predictions?.predictionQuantity() ?: 0,
                action = {
                    predictionsRepository.disconnect()
                    if (stopIds != null && active) {
                        predictionsRepository.connectV2(
                            stopIds,
                            onJoin = ::onJoin,
                            onMessage = ::onMessage,
                        )
                    }
                },
            )
        }
    }

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
