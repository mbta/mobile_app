package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun subscribeToPredictions(
    stopIds: List<String>?,
    predictionsRepository: IPredictionsRepository = koinInject()
): PredictionsStreamDataResponse? {
    var predictionsStopJoinResponse: PredictionsByStopJoinResponse? by remember {
        mutableStateOf(null)
    }

    DisposableEffect(stopIds) {
        val scope = CoroutineScope(Dispatchers.IO)
        val job =
            scope.launch {
                if (stopIds != null) {
                    predictionsRepository.connectV2(
                        stopIds,
                        { joinResult ->
                            when (joinResult) {
                                is ApiResult.Ok -> {
                                    predictionsStopJoinResponse = joinResult.data
                                }
                                is ApiResult.Error -> TODO("handle errors")
                            }
                        },
                        { messageResult ->
                            when (messageResult) {
                                is ApiResult.Ok -> {
                                    predictionsStopJoinResponse =
                                        (predictionsStopJoinResponse
                                                ?: PredictionsByStopJoinResponse(
                                                    mapOf(
                                                        messageResult.data.stopId to
                                                            messageResult.data.predictions
                                                    ),
                                                    messageResult.data.trips,
                                                    messageResult.data.vehicles
                                                ))
                                            .mergePredictions(messageResult.data)
                                }
                                is ApiResult.Error -> TODO("handle errors")
                            }
                        }
                    )
                }
            }

        onDispose {
            predictionsRepository.disconnect()
            job.cancel()
        }
    }

    return predictionsStopJoinResponse?.toPredictionsStreamDataResponse()
}
