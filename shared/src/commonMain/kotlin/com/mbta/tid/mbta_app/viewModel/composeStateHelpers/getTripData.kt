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

private fun fetchTrip(tripId: String, params: FetchParams, updateTrip: (Trip?) -> Unit) {
    CoroutineScope(params.coroutineDispatcher).launch {
        fetchApi(
            errorBannerRepo = params.errorBannerRepository,
            errorKey = "${params.errorKey}.fetchTrip",
            getData = { params.tripRepository.getTrip(tripId) },
            onSuccess = { updateTrip(it.trip) },
            onRefreshAfterError = { fetchTrip(tripId, params, updateTrip) },
        )
    }
}

private fun fetchTripSchedules(
    tripId: String,
    params: FetchParams,
    updateTripSchedules: (TripSchedulesResponse?) -> Unit,
) {
    CoroutineScope(params.coroutineDispatcher).launch {
        fetchApi(
            errorBannerRepo = params.errorBannerRepository,
            errorKey = "${params.errorKey}.fetchTripSchedules",
            getData = { params.tripRepository.getTripSchedules(tripId) },
            onSuccess = updateTripSchedules,
            onRefreshAfterError = { fetchTripSchedules(tripId, params, updateTripSchedules) },
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
    }

    tripPredictions =
        subscribeToTripPredictions(
            tripFilter?.tripId,
            errorKey,
            active,
            context,
            onPredictionMessageReceived,
            errorBannerRepository,
            tripPredictionsRepository,
        )

    vehicle =
        subscribeToVehicle(
            tripFilter?.vehicleId,
            active,
            errorKey,
            errorBannerRepository,
            vehicleRepository,
        )

    LaunchedEffect(tripFilter?.stopId) { clearAll() }
    LaunchedEffect(tripFilter?.tripId) {
        clearAll()
        tripFilter?.tripId?.let { tripId ->
            params.let { params ->
                fetchTrip(tripId, params) { trip = it }
                fetchTripSchedules(tripId, params) { tripSchedules = it }
            }
        }
    }

    LaunchedEffect(tripFilter?.vehicleId) {}

    LaunchedEffect(trip, tripSchedules, tripPredictions) {
        val resolvedTrip = trip
        result =
            if (
                tripFilter != null && resolvedTrip != null && resolvedTrip.id == tripFilter.tripId
            ) {
                TripData(tripFilter, resolvedTrip, tripSchedules, tripPredictions, true, vehicle)
            } else {
                null
            }
    }

    return result
}
