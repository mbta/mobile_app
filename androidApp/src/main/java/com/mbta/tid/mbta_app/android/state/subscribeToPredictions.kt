package com.mbta.tid.mbta_app.android.state

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

class PredictionsViewModel(
    private val predictionsRepository: IPredictionsRepository,
) : ViewModel() {
    private val _predictions: MutableLiveData<PredictionsByStopJoinResponse> = MutableLiveData()
    val predictions: LiveData<PredictionsByStopJoinResponse> = _predictions
    val predictionsFlow = predictions.asFlow().map { it.toPredictionsStreamDataResponse() }

    override fun onCleared() {
        super.onCleared()
        predictionsRepository.disconnect()
    }

    fun connect(stopIds: List<String>?) {
        if (stopIds != null) {
            predictionsRepository.connectV2(stopIds, ::handleJoinMessage, ::handlePushMessage)
        }
    }

    private fun handleJoinMessage(message: ApiResult<PredictionsByStopJoinResponse>) {
        when (message) {
            is ApiResult.Ok -> {
                _predictions.postValue(message.data)
            }
            is ApiResult.Error -> {
                Log.e(
                    "PredictionsViewModel",
                    "Predictions stream failed to join: ${message.message}"
                )
            }
        }
    }

    private fun handlePushMessage(message: ApiResult<PredictionsByStopMessageResponse>) {
        when (message) {
            is ApiResult.Ok -> {
                _predictions.postValue(
                    (_predictions.value
                            ?: PredictionsByStopJoinResponse(
                                mapOf(message.data.stopId to message.data.predictions),
                                message.data.trips,
                                message.data.vehicles
                            ))
                        .mergePredictions(message.data)
                )
            }
            is ApiResult.Error -> {
                Log.e(
                    "PredictionsViewModel",
                    "Predictions stream failed on message: ${message.message}"
                )
            }
        }
    }

    fun disconnect() {
        predictionsRepository.disconnect()
    }

    class Factory(private val predictionsRepository: IPredictionsRepository) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PredictionsViewModel(predictionsRepository) as T
        }
    }
}

@Composable
fun subscribeToPredictions(
    stopIds: List<String>?,
    predictionsRepository: IPredictionsRepository = koinInject()
): PredictionsStreamDataResponse? {
    val viewModel: PredictionsViewModel =
        viewModel(factory = PredictionsViewModel.Factory(predictionsRepository))

    LifecycleResumeEffect(key1 = stopIds) {
        viewModel.connect(stopIds)

        onPauseOrDispose { viewModel.disconnect() }
    }
    return viewModel.predictionsFlow.collectAsState(initial = null).value
}
