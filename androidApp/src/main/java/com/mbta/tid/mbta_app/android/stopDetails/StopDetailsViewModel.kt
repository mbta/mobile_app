package com.mbta.tid.mbta_app.android.stopDetails

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mbta.tid.mbta_app.android.state.ScheduleFetcher
import com.mbta.tid.mbta_app.android.state.StopPredictionsFetcher
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.StopData
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class StopDetailsViewModel(
    schedulesRepository: ISchedulesRepository,
    private val predictionsRepository: IPredictionsRepository,
    private val errorBannerRepository: IErrorBannerStateRepository,
) : ViewModel() {
    private val stopScheduleFetcher = ScheduleFetcher(schedulesRepository, errorBannerRepository)

    private val _stopData = MutableStateFlow<StopData?>(null)
    val stopData: StateFlow<StopData?> = _stopData

    private val stopPredictionsFetcher =
        StopPredictionsFetcher(
            predictionsRepository,
            errorBannerRepository,
            ::onJoinMessage,
            ::onPushMessage
        )

    fun loadStopDetails(stopId: String) {
        _stopData.value = StopData(stopId, null, null, false)
        getStopSchedules(stopId)
        joinStopPredictions(stopId)
    }

    private fun onJoinMessage(message: PredictionsByStopJoinResponse) {
        _stopData.update { it -> it?.let { StopData(it.stopId, it.schedules, message, true) } }
    }

    private fun onPushMessage(
        message: PredictionsByStopMessageResponse
    ): PredictionsByStopJoinResponse? {

        return _stopData
            ?.updateAndGet {
                it?.let {
                    StopData(
                        it.stopId,
                        it.schedules,
                        it.predictionsByStop
                            ?: PredictionsByStopJoinResponse(
                                    mapOf(it.stopId to message.predictions),
                                    message.trips,
                                    message.vehicles
                                )
                                .mergePredictions(message)
                    )
                }
            }
            ?.predictionsByStop
    }

    fun getStopSchedules(stopId: String) {
        Log.i("KB", "fetched schedules ${stopId}")

        stopScheduleFetcher.getSchedule(listOf(stopId), "StopDetailsVM.getSchedule") { schedules ->
            _stopData.update {
                it?.let {
                    StopData(it.stopId, schedules, it.predictionsByStop, it.predictionsLoaded)
                }
            }
        }
    }

    fun joinStopPredictions(stopId: String) {
        Log.i("KB", "joined stop predictions ${stopId}")

        stopPredictionsFetcher.connect(listOf(stopId))
    }

    fun rejoinStopPredictions() {

        stopData.value?.let {
            Log.i("KB", "rejoined stop predictions")
            joinStopPredictions(it.stopId)
        }
    }

    fun leaveStopPredictions() {
        Log.i("KB", "Left stop predictions")

        stopPredictionsFetcher.disconnect()
    }

    fun clearStopDetails() {
        Log.i("KB", "Cleared stop details data")
        stopPredictionsFetcher.disconnect()
        _stopData.value = null
        errorBannerRepository.clearDataError("StopDetailsVM.getSchedule")
        // TODO: leave predictions
        _stopData.value = null
    }

    fun handleStopChange(stopId: String?) {
        clearStopDetails()

        if (stopId != null) {
            loadStopDetails(stopId)
        }
    }

    fun checkPredictionsStale() {
        stopPredictionsFetcher.checkPredictionsStale(_stopData.value?.predictionsByStop)
    }

    // Clear predictions if too stale
    fun returnFromBackground() {
        _stopData.update {
            if (
                it != null &&
                    predictionsRepository.shouldForgetPredictions(
                        it?.predictionsByStop?.predictionQuantity() ?: 0
                    )
            ) {
                StopData(it.stopId, it.schedules, null, false)
            } else {
                it
            }
        }
    }
}

@Composable
/**
 * Manage stop details data and lifecycle events, including:
 * - re-fetching data when the selected stopId changes
 * - re-subscribing to predictions when returning from background
 * - periodically checking for stale predictions
 */
fun stopDetailsVMHandler(
    stopId: String?,
    viewModel: StopDetailsViewModel = koinViewModel(),
    checkPredictionsStaleInterval: Duration = 5.seconds
): StopDetailsViewModel {

    val timer = timer(checkPredictionsStaleInterval)

    LaunchedEffect(stopId) {
        CoroutineScope(Dispatchers.IO).launch { viewModel.handleStopChange(stopId) }
    }

    LifecycleResumeEffect(null) {
        viewModel.returnFromBackground()
        viewModel.rejoinStopPredictions()

        onPauseOrDispose { viewModel.leaveStopPredictions() }
    }

    LaunchedEffect(key1 = timer) { viewModel.checkPredictionsStale() }

    return viewModel
}
