package com.mbta.tid.mbta_app.android.stopDetails

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mbta.tid.mbta_app.android.state.ScheduleFetcher
import com.mbta.tid.mbta_app.android.state.StopPredictionsFetcher
import com.mbta.tid.mbta_app.android.util.fetchApi
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.StopDetailsUtils
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.hasContext
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.StopData
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripData
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.ITripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ITripRepository
import com.mbta.tid.mbta_app.repositories.IVehicleRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockScheduleRepository
import com.mbta.tid.mbta_app.repositories.MockTripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockTripRepository
import com.mbta.tid.mbta_app.repositories.MockVehicleRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

class StopDetailsViewModel(
    private val errorBannerRepository: IErrorBannerStateRepository,
    private val predictionsRepository: IPredictionsRepository,
    schedulesRepository: ISchedulesRepository,
    private val tripPredictionsRepository: ITripPredictionsRepository,
    private val tripRepository: ITripRepository,
    private val vehicleRepository: IVehicleRepository,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    companion object {
        fun mocked(
            errorBannerRepo: IErrorBannerStateRepository = MockErrorBannerStateRepository(),
            predictionsRepo: IPredictionsRepository = MockPredictionsRepository(),
            schedulesRepo: ISchedulesRepository = MockScheduleRepository(),
            tripPredictionsRepo: ITripPredictionsRepository = MockTripPredictionsRepository(),
            tripRepo: ITripRepository = MockTripRepository(),
            vehicleRepo: IVehicleRepository = MockVehicleRepository(),
            coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
        ): StopDetailsViewModel {
            return StopDetailsViewModel(
                errorBannerRepo,
                predictionsRepo,
                schedulesRepo,
                tripPredictionsRepo,
                tripRepo,
                vehicleRepo,
                coroutineDispatcher,
            )
        }

        fun mocked(
            objects: ObjectCollectionBuilder,
            errorBannerRepo: IErrorBannerStateRepository = MockErrorBannerStateRepository(),
            predictionsRepo: IPredictionsRepository =
                MockPredictionsRepository(
                    connectV2Response = PredictionsByStopJoinResponse(objects)
                ),
            schedulesRepo: ISchedulesRepository =
                MockScheduleRepository(scheduleResponse = ScheduleResponse(objects)),
            tripPredictionsRepo: ITripPredictionsRepository =
                MockTripPredictionsRepository(response = PredictionsStreamDataResponse(objects)),
            tripRepo: ITripRepository = MockTripRepository(),
            vehicleRepo: IVehicleRepository = MockVehicleRepository(),
            coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
        ) =
            mocked(
                errorBannerRepo,
                predictionsRepo,
                schedulesRepo,
                tripPredictionsRepo,
                tripRepo,
                vehicleRepo,
                coroutineDispatcher,
            )
    }

    private val stopScheduleFetcher = ScheduleFetcher(schedulesRepository, errorBannerRepository)

    private val _globalResponse = MutableStateFlow<GlobalResponse?>(null)
    val globalResponse = _globalResponse.asStateFlow()

    private val _stopData = MutableStateFlow<StopData?>(null)
    val stopData = _stopData.asStateFlow()

    private val _tripData = MutableStateFlow<TripData?>(null)
    val tripData = _tripData.asStateFlow()

    private val _tripDetailsStopList = MutableStateFlow<TripDetailsStopList?>(null)
    // not accessible via a property since it needs extra params to be kept up to date

    // The routeCardData flow is public but should only be used in stopDetailsManagedVM,
    // filtered and unfiltered stop details should each use the specific flow for their data.
    private val _routeCardData = MutableStateFlow<List<RouteCardData>?>(null)
    val routeCardData = _routeCardData.asStateFlow()

    private val _filteredRouteStopData = MutableStateFlow<RouteCardData.RouteStopData?>(null)
    val filteredRouteStopData = _filteredRouteStopData.asStateFlow()

    private val _unfilteredRouteCardData = MutableStateFlow<List<RouteCardData>?>(null)
    val unfilteredRouteCardData = _unfilteredRouteCardData.asStateFlow()

    private val _alertSummaries = MutableStateFlow<Map<String, AlertSummary?>>(emptyMap())
    val alertSummaries = _alertSummaries.asStateFlow()

    private val stopPredictionsFetcher =
        StopPredictionsFetcher(
            predictionsRepository,
            errorBannerRepository,
            ::onJoinMessage,
            ::onPushMessage,
        )

    fun setGlobalResponse(globalResponse: GlobalResponse?) {
        _globalResponse.value = globalResponse
    }

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
            .updateAndGet {
                it?.let {
                    StopData(
                        it.stopId,
                        it.schedules,
                        (it.predictionsByStop ?: PredictionsByStopJoinResponse(message))
                            .mergePredictions(message),
                        true,
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

    private fun joinTripChannels(tripFilter: TripDetailsFilter) {
        joinVehicle(tripFilter)
        joinTripPredictions(tripFilter)
    }

    private fun joinTripPredictions(tripFilter: TripDetailsFilter) {
        leaveTripPredictions()

        tripPredictionsRepository.connect(tripFilter.tripId) { outcome ->
            when (outcome) {
                // no error handling since persistent errors cause stale predictions
                is ApiResult.Ok -> {
                    _tripData.update { it?.copy(tripPredictions = outcome.data) }
                }
                is ApiResult.Error -> {}
            }
            _tripData.update { it?.copy(tripPredictionsLoaded = true) }
        }
    }

    private fun joinVehicle(tripFilter: TripDetailsFilter) {
        leaveVehicle()
        val vehicleId = tripFilter.vehicleId
        if (vehicleId == null) {
            // If the filter has a null vehicle ID, we can't join anything, clear the vehicle and
            // return
            _tripData.update { it?.copy(vehicle = null) }
            return
        } else {

            val errorKey = "TripDetailsView.joinVehicle"
            vehicleRepository.connect(vehicleId) { outcome ->
                when (outcome) {
                    is ApiResult.Ok -> {
                        _tripData.update { it?.copy(vehicle = outcome.data.vehicle) }
                        errorBannerRepository.clearDataError(errorKey)
                    }
                    is ApiResult.Error -> {
                        _tripData.update { it?.copy(vehicle = null) }

                        errorBannerRepository.setDataError(errorKey, outcome.toString()) {
                            clearAndLoadTripDetails(tripFilter)
                        }
                    }
                }
            }
        }
    }

    fun leaveStopPredictions() {
        stopPredictionsFetcher.disconnect()
    }

    fun leaveTripChannels() {
        leaveTripPredictions()
        leaveVehicle()
    }

    private fun leaveVehicle() {
        vehicleRepository.disconnect()
    }

    private fun leaveTripPredictions() {
        tripPredictionsRepository.disconnect()
    }

    fun rejoinStopPredictions() {
        stopData.value?.let { joinStopPredictions(it.stopId) }
    }

    fun rejoinTripChannels() {
        tripData.value?.let { joinTripChannels(it.tripFilter) }
    }

    fun setAlertSummaries(alertSummaries: Map<String, AlertSummary?>) {
        _alertSummaries.value = alertSummaries
    }

    fun setRouteCardData(routeCardData: List<RouteCardData>?) {
        _routeCardData.value = routeCardData
    }

    fun clearFilteredRouteStopData() {
        _filteredRouteStopData.value = null
    }

    fun setFilteredRouteStopData(stopData: RouteCardData.RouteStopData) {
        _filteredRouteStopData.value = stopData
    }

    private fun clearUnfilteredRouteCardData() {
        _unfilteredRouteCardData.value = null
    }

    fun setUnfilteredRouteCardData(routeCardData: List<RouteCardData>) {
        _unfilteredRouteCardData.value = routeCardData
    }

    private fun clearStopDetails() {
        stopPredictionsFetcher.disconnect()
        _stopData.value = null
        errorBannerRepository.clearDataError("StopDetailsVM.getSchedule")
    }

    fun clearTripDetails() {
        leaveTripChannels()
        _tripData.value = null
        errorBannerRepository.clearDataError("TripDetailsView.joinVehicle")
        errorBannerRepository.clearDataError("TripDetailsView.loadTripSchedules")
        errorBannerRepository.clearDataError("TripDetailsView.loadTrip")
    }

    @Composable
    fun getTripDetailsStopList(
        tripFilter: TripDetailsFilter?,
        allAlerts: AlertsStreamDataResponse?,
        globalResponse: GlobalResponse?,
    ): StateFlow<TripDetailsStopList?> {
        val tripData = this.tripData.collectAsState().value
        LaunchedEffect(tripFilter, tripData, allAlerts, globalResponse) {
            _tripDetailsStopList.value =
                if (
                    tripFilter != null &&
                        tripData != null &&
                        tripData.tripFilter == tripFilter &&
                        tripData.tripPredictionsLoaded &&
                        globalResponse != null
                ) {
                    TripDetailsStopList.fromPieces(
                        tripData.trip,
                        tripData.tripSchedules,
                        tripData.tripPredictions,
                        tripData.vehicle,
                        allAlerts,
                        globalResponse,
                    )
                } else {
                    null
                }
        }
        return this._tripDetailsStopList.asStateFlow()
    }

    private fun clearAndLoadTripDetails(tripFilter: TripDetailsFilter) {
        CoroutineScope(coroutineDispatcher).launch {
            clearTripDetails()
            loadTripDetails(tripFilter)
            joinTripChannels(tripFilter)
        }
    }

    private suspend fun loadTripDetails(tripFilter: TripDetailsFilter) {
        val tripResult = CoroutineScope(coroutineDispatcher).async { loadTrip(tripFilter) }
        val schedulesResult =
            CoroutineScope(coroutineDispatcher).async { loadTripSchedules(tripFilter) }

        val trip = tripResult.await()
        val schedules = schedulesResult.await()
        if (trip == null) {
            _tripData.value = null
        } else {
            _tripData.update {
                TripData(
                    tripFilter = tripFilter,
                    trip = trip,
                    tripSchedules = schedules,
                    tripPredictions = null,
                    vehicle = null,
                )
            }
        }
    }

    private suspend fun loadTrip(tripFilter: TripDetailsFilter): Trip? {
        val errorKey = "TripDetailsView.loadTrip"

        return withContext(coroutineDispatcher) {
            when (val response = tripRepository.getTrip(tripFilter.tripId)) {
                is ApiResult.Ok -> {
                    errorBannerRepository.clearDataError(errorKey)
                    response.data.trip
                }
                is ApiResult.Error -> {
                    Log.e("StopDetailsViewModel", "loadTrip failed: $response")
                    errorBannerRepository.setDataError(errorKey, details = response.toString()) {
                        clearAndLoadTripDetails(tripFilter)
                    }
                    null
                }
            }
        }
    }

    private suspend fun loadTripSchedules(tripFilter: TripDetailsFilter): TripSchedulesResponse? {
        return CoroutineScope(coroutineDispatcher)
            .async {
                var result: TripSchedulesResponse? = null
                fetchApi(
                    errorBannerRepository,
                    "TripDetailsView.loadTripSchedules",
                    { tripRepository.getTripSchedules(tripFilter.tripId) },
                    { schedules -> result = schedules },
                    { clearAndLoadTripDetails(tripFilter) },
                )
                result
            }
            .await()
    }

    fun handleStopChange(stopId: String?) {
        clearStopDetails()
        clearFilteredRouteStopData()
        clearUnfilteredRouteCardData()

        if (stopId != null) {
            loadStopDetails(stopId)
        }
    }

    fun handleTripFilterChange(tripFilter: TripDetailsFilter?) {
        if (tripFilter == null) {
            clearTripDetails()
            return
        }

        /*
        Callback to invoke after the _tripData value is atomically updated. These side effects
        must not need to be to atomically processed with the value change.
         */
        var filterChangeSideEffects: () -> Unit = {}
        _tripData.update { currentTripData ->
            if (currentTripData != null) {
                val currentFilter = currentTripData.tripFilter
                if (
                    currentFilter.tripId == tripFilter.tripId &&
                        currentFilter.vehicleId == tripFilter.vehicleId
                ) {
                    // If the filter changed but the trip and vehicle are the same, replace the
                    // filter but keep all the data
                    currentTripData.copy(tripFilter = tripFilter, tripPredictionsLoaded = true)
                } else if (currentFilter.tripId == tripFilter.tripId) {
                    // If only the vehicle changed but the trip is the same, clear and reload only
                    // the vehicle, keep the prediction channel open and copy static trip data
                    // into new trip data
                    leaveVehicle()
                    filterChangeSideEffects = { joinVehicle(tripFilter) }

                    currentTripData.copy(tripFilter = tripFilter, vehicle = null)
                } else if (currentFilter.vehicleId == tripFilter.vehicleId) {
                    // If only the trip changed but the vehicle is the same, clear and reload only
                    // the trip details, keep the vehicle channel open and copy the last
                    // vehicle into the new trip data
                    val currentVehicle = currentTripData.vehicle
                    leaveTripPredictions()

                    filterChangeSideEffects = {
                        CoroutineScope(coroutineDispatcher).launch {
                            loadTripDetails(tripFilter)
                            _tripData.update { it?.copy(vehicle = currentVehicle) }
                            joinTripPredictions(tripFilter)
                        }
                    }

                    null
                } else {
                    // If current trip data exists but neither the trip ID or vehicle ID match,
                    // fall through and reload everything
                    filterChangeSideEffects = { clearAndLoadTripDetails(tripFilter) }

                    null
                }
            } else {
                filterChangeSideEffects = { clearAndLoadTripDetails(tripFilter) }

                null
            }
        }
        filterChangeSideEffects()
    }

    fun checkStopPredictionsStale() {
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
        _tripData.update {
            if (
                it != null &&
                    tripPredictionsRepository.shouldForgetPredictions(
                        it.tripPredictions?.predictionQuantity() ?: 0
                    )
            ) {
                it.copy(tripPredictions = null)
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
    updateStopFilter: (String, StopDetailsFilter?) -> Unit,
    updateTripFilter: (String, TripDetailsFilter?) -> Unit,
    setMapSelectedVehicle: (Vehicle?) -> Unit,
    now: EasternTimeInstant? = null,
    viewModel: StopDetailsViewModel = koinViewModel(),
    checkPredictionsStaleInterval: Duration = 5.seconds,
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    clock: Clock = koinInject(),
): StopDetailsViewModel {
    val now = now ?: EasternTimeInstant(clock.now())
    val stopId = filters?.stopId
    val timer by timer(checkPredictionsStaleInterval)

    val stopData by viewModel.stopData.collectAsState()

    val routeCardData by viewModel.routeCardData.collectAsState()

    var wasInBackground by rememberSaveable { mutableStateOf(false) }

    fun stopDataForRoute(
        routeId: String,
        routeCardData: List<RouteCardData>?,
    ): RouteCardData.RouteStopData? =
        if (routeCardData.hasContext(RouteCardData.Context.StopDetailsFiltered))
            routeCardData?.find { it.lineOrRoute.id == routeId }?.stopData?.get(0)
        else null

    LaunchedEffect(stopId) { viewModel.handleStopChange(stopId) }
    LaunchedEffect(filters?.tripFilter) { viewModel.handleTripFilterChange(filters?.tripFilter) }

    LifecycleResumeEffect(null) {
        // we don’t want to return and rejoin on first appearance, only on actual resume, because
        // the handleStopChange/handleTripFilterChange will handle initial joins
        if (wasInBackground) {
            wasInBackground = false
            viewModel.returnFromBackground()
            viewModel.rejoinStopPredictions()
            viewModel.rejoinTripChannels()
        }

        onPauseOrDispose {
            wasInBackground = true
            viewModel.leaveStopPredictions()
            viewModel.leaveTripChannels()
        }
    }

    LaunchedEffect(globalResponse) { viewModel.setGlobalResponse(globalResponse) }

    LaunchedEffect(stopId, globalResponse, stopData, filters, alertData, now) {
        val schedules = stopData?.schedules
        withContext(Dispatchers.Default) {
            viewModel.setRouteCardData(
                if (
                    globalResponse != null &&
                        stopId != null &&
                        stopId == stopData?.stopId &&
                        schedules != null &&
                        stopData?.predictionsLoaded == true
                ) {
                    RouteCardData.routeCardsForStopList(
                        listOf(stopId) + globalResponse.getStop(stopId)?.childStopIds.orEmpty(),
                        globalResponse,
                        sortByDistanceFrom = null,
                        schedules,
                        stopData?.predictionsByStop?.toPredictionsStreamDataResponse(),
                        alertData,
                        now,
                        context =
                            if (filters.stopFilter != null)
                                RouteCardData.Context.StopDetailsFiltered
                            else RouteCardData.Context.StopDetailsUnfiltered,
                    )
                } else null
            )
        }
    }

    LaunchedEffect(timer) { viewModel.checkStopPredictionsStale() }

    LaunchedEffect(filters) {
        if (filters != null) {
            val autoTripFilter =
                StopDetailsUtils.autoTripFilter(
                    routeCardData,
                    filters.stopFilter,
                    filters.tripFilter,
                    now,
                    globalResponse,
                )

            if (autoTripFilter != filters.tripFilter) {
                updateTripFilter(filters.stopId, autoTripFilter)
            }
        }

        // If a route ID filter exists, we want to update the filtered stop data for that route,
        // if not, we want to clear any previously set filtered stop data
        val filteredRouteId = filters?.stopFilter?.routeId
        if (filteredRouteId != null) {
            stopDataForRoute(filteredRouteId, routeCardData)?.let { stopData ->
                viewModel.setFilteredRouteStopData(stopData)
            }
        } else {
            viewModel.clearFilteredRouteStopData()
        }
    }

    LaunchedEffect(routeCardData) {
        if (filters != null && routeCardData != null) {
            val stopFilter = filters.stopFilter ?: StopDetailsUtils.autoStopFilter(routeCardData)

            if (stopFilter != filters.stopFilter) {
                updateStopFilter(filters.stopId, stopFilter)
            }
            // Wait until auto stopFilter has been applied to apply the trip filter
            // to ensure that tripFilter doesn't overwrite the new stopFilter
            if (filters.stopFilter == stopFilter) {
                val autoTripFilter =
                    StopDetailsUtils.autoTripFilter(
                        routeCardData,
                        filters.stopFilter,
                        filters.tripFilter,
                        now,
                        globalResponse,
                    )

                if (autoTripFilter != filters.tripFilter) {
                    updateTripFilter(filters.stopId, autoTripFilter)
                }
            }
        }

        // If a route ID filter exists, we want to update the filtered stop data for that route,
        // otherwise, update the unfiltered route card data.
        val filteredRouteId = filters?.stopFilter?.routeId
        if (filteredRouteId != null) {
            stopDataForRoute(filteredRouteId, routeCardData)?.let { stopData ->
                viewModel.setFilteredRouteStopData(stopData)
            }
        } else {
            routeCardData?.let {
                if (it.hasContext(RouteCardData.Context.StopDetailsUnfiltered)) {
                    viewModel.setUnfilteredRouteCardData(it)
                }
            }
        }
    }

    val tripData by viewModel.tripData.collectAsState()
    val vehicle = tripData?.vehicle

    LaunchedEffect(filters?.tripFilter, vehicle) {
        if (vehicle?.id == filters?.tripFilter?.vehicleId) {
            setMapSelectedVehicle(vehicle)
        } else {
            setMapSelectedVehicle(null)
        }
    }

    return viewModel
}
