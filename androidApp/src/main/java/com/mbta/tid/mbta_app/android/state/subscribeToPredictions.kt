package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mbta.tid.mbta_app.android.util.TimerViewModel
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.internal.notifyAll
import org.koin.compose.koinInject

class PredictionsViewModel(
    private val stopIds: List<String>,
    private val predictionsRepository: IPredictionsRepository,
    private val timerViewModel: TimerViewModel
) : ViewModel() {
    private val _predictions: MutableLiveData<PredictionsByStopJoinResponse> = MutableLiveData()
    val predictions: LiveData<PredictionsByStopJoinResponse> = _predictions

    init {
        CoroutineScope(Dispatchers.IO).launch {
            if (stopIds.size > 0) {
                connectToPredictions()
            }
            timerViewModel.timerFlow.collect {
                synchronized(predictions) { predictions.notifyAll() }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        predictionsRepository.disconnect()
    }

    private fun connectToPredictions() {
        predictionsRepository.connectV2(
            stopIds,
            {
                when (it) {
                    is ApiResult.Ok -> {
                        _predictions.postValue(it.data)
                    }
                    is ApiResult.Error -> {
                        /* TODO("handle errors") */
                    }
                }
            },
            {
                when (it) {
                    is ApiResult.Ok -> {
                        _predictions.postValue(
                            (_predictions.value
                                    ?: PredictionsByStopJoinResponse(
                                        mapOf(it.data.stopId to it.data.predictions),
                                        it.data.trips,
                                        it.data.vehicles
                                    ))
                                .mergePredictions(it.data)
                        )
                    }
                    is ApiResult.Error -> {
                        /* TODO("handle errors") */
                    }
                }
            }
        )
    }
}

@Composable
fun subscribeToPredictions(
    stopIds: List<String>?,
    predictionsRepository: IPredictionsRepository = koinInject()
): PredictionsStreamDataResponse? {
    val timerViewModel = remember { TimerViewModel(1.seconds) }
    val viewModel: PredictionsViewModel =
        remember(stopIds) {
            PredictionsViewModel(stopIds ?: emptyList(), predictionsRepository, timerViewModel)
        }
    return viewModel.predictions
        .asFlow()
        .map { it.toPredictionsStreamDataResponse() }
        .collectAsState(initial = null)
        .value
}
