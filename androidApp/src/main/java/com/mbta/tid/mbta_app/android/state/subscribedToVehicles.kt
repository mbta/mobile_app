package com.mbta.tid.mbta_app.android.state

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IVehiclesRepository
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable data class VehiclesTopic(val routeId: String, val directionId: Int)

class VehiclesViewModel(
    private val vehiclesRepository: IVehiclesRepository,
) : ViewModel() {
    private val _vehicles: MutableLiveData<VehiclesStreamDataResponse> = MutableLiveData()
    private val vehicles: LiveData<VehiclesStreamDataResponse> = _vehicles
    val vehiclesFlow = vehicles.asFlow()

    init {
        Log.i("KB", "init vehicles view model")
    }

    override fun onCleared() {
        super.onCleared()
        Log.i("KB", "OnCleared Called")

        _vehicles.value = VehiclesStreamDataResponse(vehicles = emptyMap())
        vehiclesRepository.disconnect()
    }

    fun connectToVehicles(topic: VehiclesTopic?) {
        Log.i("KB", "connectToVehicles called")
        disconnect()

        if (topic != null) {
            Log.i("KB", "connected")
            vehiclesRepository.connect(topic.routeId, topic.directionId) {
                when (it) {
                    is ApiResult.Ok -> {
                        Log.i("KB", "Received Vehicles ${it.data.vehicles.values.first().routeId}")
                        _vehicles.postValue(it.data)
                    }
                    is ApiResult.Error -> {
                        Log.e("VehiclesViewModel", "Vehicle stream failed: ${it.message}")
                    }
                }
            }
        }
    }

    fun disconnect() {
        Log.i("KB", "disconnected")
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
    topic: VehiclesTopic?,
    vehiclesRepository: IVehiclesRepository = koinInject()
): List<Vehicle> {
    Log.i("KB", "new topic ${topic}")
    val viewModel: VehiclesViewModel =
        viewModel(factory = VehiclesViewModel.Factory(vehiclesRepository))

    val vehicleData = viewModel?.vehiclesFlow?.collectAsState(initial = null)?.value

    LifecycleResumeEffect(key1 = topic) {
        Log.i("KB", "resumed")
        viewModel.connectToVehicles(topic)

        onPauseOrDispose {
            Log.i("KB", "paused")
            viewModel.disconnect()
        }
    }

    return vehicleData?.vehicles?.values?.toList() ?: emptyList()
}