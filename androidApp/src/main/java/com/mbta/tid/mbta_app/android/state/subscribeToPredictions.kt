package com.mbta.tid.mbta_app.android.state

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent

class StopPredictionsFetcher(
    private val predictionsRepository: IPredictionsRepository,
    private val errorRepository: IErrorBannerStateRepository,
    private val onJoinResponse: (PredictionsByStopJoinResponse) -> Unit,
    private val onPushMessage: (PredictionsByStopMessageResponse) -> PredictionsByStopJoinResponse?
) {

    private var currentStopIds: List<String>? = null

    fun connect(stopIds: List<String>?) {
        currentStopIds = stopIds
        if (stopIds != null) {
            predictionsRepository.connectV2(stopIds, ::handleJoinMessage, ::handlePushMessage)
        }
    }

    private fun handleJoinMessage(message: ApiResult<PredictionsByStopJoinResponse>) {
        when (message) {
            is ApiResult.Ok -> {
                onJoinResponse(message.data)
                checkPredictionsStale(message.data)
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
                val latestPredictions = onPushMessage(message.data)
                latestPredictions?.let { checkPredictionsStale(it) }
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

    fun checkPredictionsStale(predictions: PredictionsByStopJoinResponse?) {
        CoroutineScope(Dispatchers.IO).launch {
            predictionsRepository.lastUpdated?.let { lastPredictions ->
                errorRepository.checkPredictionsStale(
                    predictionsLastUpdated = lastPredictions,
                    predictionQuantity = predictions?.predictionQuantity() ?: 0,
                    action = {
                        disconnect()
                        connect(currentStopIds)
                    }
                )
            }
        }
    }
}

class PredictionsViewModel(
    private val predictionsRepository: IPredictionsRepository,
    private val errorBannerViewModel: ErrorBannerViewModel
) : KoinComponent, ViewModel() {
    private val _predictions = MutableStateFlow<PredictionsByStopJoinResponse?>(null)
    val predictions: StateFlow<PredictionsByStopJoinResponse?> = _predictions
    val predictionsFlow =
        predictions.debounce(0.1.seconds).map { it?.toPredictionsStreamDataResponse() }

    private val stopPredictionsFetcher =
        StopPredictionsFetcher(
            predictionsRepository,
            errorBannerViewModel.errorRepository,
            ::onJoinResponse,
            ::onPushMessage
        )

    fun onJoinResponse(joinResponse: PredictionsByStopJoinResponse) {
        _predictions.value = joinResponse
        errorBannerViewModel.loadingWhenPredictionsStale = false
    }

    fun onPushMessage(
        pushMessage: PredictionsByStopMessageResponse
    ): PredictionsByStopJoinResponse? {
        return _predictions.updateAndGet { currentPredictions ->
            val currentPredictions =
                currentPredictions ?: PredictionsByStopJoinResponse(mapOf(), mapOf(), mapOf())
            currentPredictions.mergePredictions(pushMessage)
        }
    }

    fun connect(stopIds: List<String>?) {
        stopPredictionsFetcher.connect(stopIds)
    }

    fun disconnect() {
        stopPredictionsFetcher.disconnect()
        errorBannerViewModel.loadingWhenPredictionsStale = true
    }

    override fun onCleared() {
        super.onCleared()
        stopPredictionsFetcher.disconnect()
    }

    fun reset() {
        _predictions.value = null
    }

    fun checkPredictionsStale() {
        stopPredictionsFetcher.checkPredictionsStale(_predictions.value)
    }

    class Factory(
        private val predictionsRepository: IPredictionsRepository,
        private val errorBannerViewModel: ErrorBannerViewModel
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PredictionsViewModel(predictionsRepository, errorBannerViewModel) as T
        }
    }
}

@Composable
fun subscribeToPredictions(
    stopIds: List<String>?,
    predictionsRepository: IPredictionsRepository = koinInject(),
    errorBannerViewModel: ErrorBannerViewModel,
    checkPredictionsStaleInterval: Duration = 5.seconds
): PredictionsViewModel {
    val viewModel: PredictionsViewModel =
        viewModel(
            factory = PredictionsViewModel.Factory(predictionsRepository, errorBannerViewModel)
        )

    val timer = timer(checkPredictionsStaleInterval)

    LifecycleResumeEffect(key1 = stopIds) {
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.checkPredictionsStale()
            viewModel.connect(stopIds)
        }

        onPauseOrDispose { viewModel.disconnect() }
    }

    LaunchedEffect(key1 = timer) { viewModel.checkPredictionsStale() }

    return viewModel
}
