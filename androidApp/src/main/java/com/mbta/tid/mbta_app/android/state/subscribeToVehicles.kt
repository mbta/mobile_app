package com.mbta.tid.mbta_app.android.state

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteDirection
import com.mbta.tid.mbta_app.model.StopDetailsUtils
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IVehiclesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class VehiclesViewModel(private val vehiclesRepository: IVehiclesRepository) : ViewModel() {
    private val _vehicles = MutableStateFlow<VehiclesStreamDataResponse?>(null)
    val vehiclesFlow: StateFlow<VehiclesStreamDataResponse?> = _vehicles

    override fun onCleared() {
        super.onCleared()

        _vehicles.value = VehiclesStreamDataResponse(vehicles = emptyMap())
        vehiclesRepository.disconnect()
    }

    fun connectToVehicles(routeDirection: RouteDirection?) {
        disconnect()

        if (routeDirection != null) {
            vehiclesRepository.connect(routeDirection.routeId, routeDirection.directionId) {
                when (it) {
                    is ApiResult.Ok -> {
                        _vehicles.value = it.data
                    }
                    is ApiResult.Error -> {
                        Log.e("VehiclesViewModel", "Vehicle stream failed: ${it.message}")
                    }
                }
            }
        }
    }

    fun disconnect() {
        _vehicles.value = VehiclesStreamDataResponse(vehicles = emptyMap())
        vehiclesRepository.disconnect()
    }

    class Factory(private val vehiclesRepository: IVehiclesRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VehiclesViewModel(vehiclesRepository) as T
        }
    }
}

@Composable
fun subscribeToVehicles(
    routeDirection: RouteDirection?,
    routeCardData: List<RouteCardData>?,
    vehiclesRepository: IVehiclesRepository = koinInject(),
): List<Vehicle> {
    val viewModel: VehiclesViewModel =
        viewModel(factory = VehiclesViewModel.Factory(vehiclesRepository))

    val vehicleData = viewModel.vehiclesFlow.collectAsState(initial = null).value

    LifecycleResumeEffect(routeDirection) {
        CoroutineScope(Dispatchers.IO).launch { viewModel.connectToVehicles(routeDirection) }

        onPauseOrDispose { viewModel.disconnect() }
    }

    return vehicleData
        ?.let { response ->
            routeCardData?.let { StopDetailsUtils.filterVehiclesByUpcoming(it, response) }
                ?: response.vehicles
        }
        ?.values
        ?.toList() ?: emptyList()
}
