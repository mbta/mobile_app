package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mbta.tid.mbta_app.android.state.ScheduleFetcher
import com.mbta.tid.mbta_app.android.state.StopPredictionsFetcher
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.StopData
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.androidx.compose.koinViewModel

class StopDetailsViewModel(
    schedulesRepository: ISchedulesRepository,
    private val predictionsRepository: IPredictionsRepository,
    private val errorBannerRepository: IErrorBannerStateRepository,
) : ViewModel() {

    private val stopScheduleFetcher = ScheduleFetcher(schedulesRepository, errorBannerRepository)

    private val _stopData = MutableStateFlow<StopData?>(null)
    val stopData: StateFlow<StopData?> = _stopData

    private val _stopDepartures = MutableStateFlow<StopDetailsDepartures?>(null)
    val stopDepartures: StateFlow<StopDetailsDepartures?> = _stopDepartures

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
                        (it.predictionsByStop
                                ?: PredictionsByStopJoinResponse(
                                    mapOf(it.stopId to message.predictions),
                                    message.trips,
                                    message.vehicles
                                ))
                            .mergePredictions(message)
                    )
                }
            }
            ?.predictionsByStop
    }

    private fun getStopSchedules(stopId: String) {
        stopScheduleFetcher.getSchedule(listOf(stopId), "StopDetailsVM.getSchedule") { schedules ->
            _stopData.update {
                it?.let {
                    StopData(it.stopId, schedules, it.predictionsByStop, it.predictionsLoaded)
                }
            }
        }
    }

    private fun joinStopPredictions(stopId: String) {
        stopPredictionsFetcher.connect(listOf(stopId))
    }

    fun rejoinStopPredictions() {

        stopData.value?.let { joinStopPredictions(it.stopId) }
    }

    fun leaveStopPredictions() {
        stopPredictionsFetcher.disconnect()
    }

    fun setDepartures(departures: StopDetailsDepartures?) {
        _stopDepartures.value = departures
    }

    private fun clearStopDetails() {
        stopPredictionsFetcher.disconnect()
        _stopData.value = null
        errorBannerRepository.clearDataError("StopDetailsVM.getSchedule")
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
 * - refetching data when the selected stopId changes
 * - resubscribing to predictions when returning from background
 * - periodically checking for stale predictions
 */
fun stopDetailsManagedVM(
    filters: StopDetailsPageFilters?,
    globalResponse: GlobalResponse?,
    alertData: AlertsStreamDataResponse?,
    pinnedRoutes: Set<String>,
    updateStopFilter: (String, StopDetailsFilter?) -> Unit,
    updateTripFilter: (String, TripDetailsFilter?) -> Unit,
    now: Instant = Clock.System.now(),
    viewModel: StopDetailsViewModel = koinViewModel(),
    checkPredictionsStaleInterval: Duration = 5.seconds
): StopDetailsViewModel {
    val stopId = filters?.stopId
    val timer = timer(checkPredictionsStaleInterval)

    val stopData = viewModel.stopData.collectAsState()

    val departures by viewModel.stopDepartures.collectAsState()

    LaunchedEffect(stopId) { viewModel.handleStopChange(stopId) }
    LifecycleResumeEffect(null) {
        viewModel.returnFromBackground()
        viewModel.rejoinStopPredictions()

        onPauseOrDispose { viewModel.leaveStopPredictions() }
    }

    LaunchedEffect(stopId, globalResponse, stopData, filters, alertData, pinnedRoutes, now) {
        withContext(Dispatchers.Default) {
            val departures: StopDetailsDepartures? =
                if (globalResponse != null && stopId != null) {
                    StopDetailsDepartures.fromData(
                        stopId,
                        globalResponse,
                        stopData.value?.schedules,
                        stopData.value?.predictionsByStop?.toPredictionsStreamDataResponse(),
                        alertData,
                        pinnedRoutes,
                        now,
                        useTripHeadsigns = false,
                    )
                } else null
            viewModel.setDepartures(departures)
        }
    }

    LaunchedEffect(key1 = timer) { viewModel.checkPredictionsStale() }

    LaunchedEffect(filters) {
        if (filters != null) {
            val autoTripFilter =
                departures?.autoTripFilter(filters.stopFilter, filters.tripFilter, now)

            if (autoTripFilter != filters.tripFilter) {

                updateTripFilter(filters.stopId, autoTripFilter)
            }
        }
    }

    LaunchedEffect(departures) {
        if (filters != null && departures != null) {
            val stopFilter = filters.stopFilter ?: departures?.autoStopFilter()

            if (stopFilter != filters.stopFilter) {
                updateStopFilter(filters.stopId, stopFilter)
            }
            // Wait until auto stopFilter has been applied to apply the trip filter
            // to ensure that tripFilter doesn't overwrite the new stopFilter
            if (filters.stopFilter == stopFilter) {
                val autoTripFilter =
                    departures?.autoTripFilter(filters.stopFilter, filters.tripFilter, now)

                if (autoTripFilter != filters.tripFilter) {
                    updateTripFilter(filters.stopId, autoTripFilter)
                }
            }
        }
    }

    return viewModel
}
