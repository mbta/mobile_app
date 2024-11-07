package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsResponse
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun subscribeToPredictions(
    stopIds: List<String>?,
    predictionsRepository: IPredictionsRepository = koinInject()
): PredictionsResponse? {
    var predictions: PredictionsResponse? by remember { mutableStateOf(null) }
    DisposableEffect(stopIds) {
        val scope = CoroutineScope(Dispatchers.IO)
        val job =
            scope.launch {
                if (stopIds != null) {
                    predictionsRepository.connectV2(
                        stopIds,
                        {
                            when (it) {
                                is ApiResult.Ok -> predictions = it.data
                                is ApiResult.Error -> TODO("handle errors")
                            }
                        }
                    ) {
                        when (it) {
                            is ApiResult.Ok ->
                                predictions =
                                    predictions?.mergePredictions(it.data)
                                        ?: PredictionsByStopJoinResponse(
                                            mutableMapOf(it.data.stopId to it.data.predictions),
                                            it.data.trips,
                                            it.data.vehicles
                                        )
                            is ApiResult.Error -> TODO("handle errors")
                        }
                    }
                }
            }

        onDispose {
            predictionsRepository.disconnect()
            job.cancel()
        }
    }

    return predictions
}
