package com.mbta.tid.mbta_app.viewModel.composeStateHelpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripData
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.ITripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ITripRepository
import com.mbta.tid.mbta_app.repositories.IVehicleRepository
import com.mbta.tid.mbta_app.viewModel.TripDetailsViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private fun fetchTripErrorKey(prefix: String) = "${prefix}.fetchTrip"

private fun fetchTrip(
    tripId: String,
    params: FetchParams,
    onError: () -> Unit,
    updateTrip: (Trip?) -> Unit,
) {
    CoroutineScope(params.coroutineDispatcher).launch {
        fetchApi(
            errorBannerRepo = params.errorBannerRepository,
            errorKey = fetchTripErrorKey(params.errorKey),
            getData = { params.tripRepository.getTrip(tripId) },
            onSuccess = { updateTrip(it.trip) },
            onRefreshAfterError = { fetchTrip(tripId, params, onError, updateTrip) },
            onError = { onError() },
        )
    }
}

private fun fetchTripSchedulesErrorKey(prefix: String) = "${prefix}.fetchTripSchedules"

private fun fetchTripSchedules(
    tripId: String,
    params: FetchParams,
    onError: () -> Unit,
    updateTripSchedules: (TripSchedulesResponse?) -> Unit,
) {
    CoroutineScope(params.coroutineDispatcher).launch {
        fetchApi(
            errorBannerRepo = params.errorBannerRepository,
            errorKey = fetchTripSchedulesErrorKey(params.errorKey),
            getData = { params.tripRepository.getTripSchedules(tripId) },
            onSuccess = updateTripSchedules,
            onRefreshAfterError = {
                fetchTripSchedules(tripId, params, onError, updateTripSchedules)
            },
            onError = { onError() },
        )
    }
}

private data class FetchParams(
    val errorKey: String,
    val coroutineDispatcher: CoroutineDispatcher,
    val errorBannerRepository: IErrorBannerStateRepository,
    val tripRepository: ITripRepository,
)

@Composable
internal fun getTripData(
    tripFilter: TripDetailsPageFilter?,
    active: Boolean,
    context: TripDetailsViewModel.Context?,
    onPredictionMessageReceived: () -> Unit,
    errorKey: String,
    coroutineDispatcher: CoroutineDispatcher,
    errorBannerRepository: IErrorBannerStateRepository = koinInject(),
    tripPredictionsRepository: ITripPredictionsRepository = koinInject(),
    tripRepository: ITripRepository = koinInject(),
    vehicleRepository: IVehicleRepository = koinInject(),
): TripData? {
    val errorKey = "$errorKey.getTripData"

    var trip: Trip? by remember { mutableStateOf(null) }
    var tripSchedules: TripSchedulesResponse? by remember { mutableStateOf(null) }
    var tripPredictions: PredictionsStreamDataResponse? by remember { mutableStateOf(null) }
    var vehicle: Vehicle? by remember { mutableStateOf(null) }
    var tripLoading: Boolean by remember { mutableStateOf(true) }

    var result: TripData? by remember { mutableStateOf(null) }

    val params =
        remember(errorKey, coroutineDispatcher, errorBannerRepository, tripRepository) {
            FetchParams(errorKey, coroutineDispatcher, errorBannerRepository, tripRepository)
        }

    fun clearAll() {
        trip = null
        tripSchedules = null
        tripPredictions = null
        vehicle = null
        tripLoading = true
    }

    val shouldTryLoadingTrip = trip != null || tripLoading

    fun fetchStaticData(tripId: String) {
        fetchTrip(
            tripId,
            params,
            {
                tripLoading = false
                if (trip == null && vehicle != null) {
                    errorBannerRepository.clearDataError(fetchTripErrorKey(errorKey))
                }
            },
        ) {
            tripLoading = false
            trip = it
        }
        fetchTripSchedules(
            tripId,
            params,
            {
                if (trip == null && vehicle != null) {
                    errorBannerRepository.clearDataError(fetchTripSchedulesErrorKey(errorKey))
                }
            },
        ) {
            tripSchedules = it
        }
    }

    tripPredictions =
        if (shouldTryLoadingTrip)
            subscribeToTripPredictions(
                tripFilter?.tripId,
                errorKey,
                active,
                context,
                onPredictionMessageReceived,
                errorBannerRepository,
                tripPredictionsRepository,
            )
        else null

    vehicle =
        subscribeToVehicle(
            tripFilter?.vehicleId,
            errorKey,
            active,
            errorBannerRepository,
            vehicleRepository,
        )

    LaunchedEffect(shouldTryLoadingTrip) {
        if (!shouldTryLoadingTrip) {
            errorBannerRepository.clearDataError(fetchTripErrorKey(errorKey))
            errorBannerRepository.clearDataError(fetchTripSchedulesErrorKey(errorKey))
            errorBannerRepository.clearDataError(tripPredictionsErrorKey(errorKey))
        }
    }

    LaunchedEffect(tripFilter?.tripId, active) {
        clearAll()
        if (active) {
            tripFilter?.tripId?.let { tripId -> if (shouldTryLoadingTrip) fetchStaticData(tripId) }
        }
    }

    LaunchedEffect(vehicle) {
        // If a trip is not loaded but the vehicle has updated, try loading the trip again
        if (!shouldTryLoadingTrip && active) {
            tripFilter?.tripId?.let { tripId -> fetchStaticData(tripId) }
        }
    }

    LaunchedEffect(tripFilter, trip, tripSchedules, tripPredictions, vehicle, context) {
        val resolvedTrip = trip
        result =
            if (
                tripFilter != null &&
                    resolvedTrip != null &&
                    resolvedTrip.id == tripFilter.tripId &&
                    (tripFilter.vehicleId == null || tripFilter.vehicleId == vehicle?.id)
            ) {
                TripData(tripFilter, resolvedTrip, tripSchedules, tripPredictions, true, vehicle)
            } else if (tripFilter != null && vehicle != null && !tripLoading) {
                TripData(tripFilter, null, null, null, true, vehicle)
            } else {
                null
            }
    }

    return result
}
