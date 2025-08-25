package com.mbta.tid.mbta_app.viewModel.composeStateHelpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.VehicleStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IVehicleRepository
import org.koin.compose.koinInject

@Composable
internal fun subscribeToVehicle(
    vehicleId: String?,
    active: Boolean,
    errorKey: String,
    errorBannerRepository: IErrorBannerStateRepository = koinInject(),
    vehicleRepository: IVehicleRepository = koinInject(),
): Vehicle? {
    var vehicle: Vehicle? by remember { mutableStateOf(null) }
    val errorKey = "$errorKey.subscribeToVehicle"

    fun connect(vehicleId: String?, onReceive: (ApiResult<VehicleStreamDataResponse>) -> Unit) {
        vehicleRepository.disconnect()
        vehicleId?.let { vehicleRepository.connect(it, onReceive) }
    }

    fun onReceive(message: ApiResult<VehicleStreamDataResponse>) {
        when (message) {
            is ApiResult.Ok -> {
                errorBannerRepository.clearDataError(errorKey)
                vehicle = message.data.vehicle
            }
            is ApiResult.Error -> {
                errorBannerRepository.setDataError(errorKey, message.toString()) {
                    connect(vehicleId, ::onReceive)
                }
                println("Trip predictions stream failed to join: ${message.message}")
                vehicle = null
            }
        }
    }

    DisposableEffect(vehicleId, active) {
        vehicle = null
        connect(vehicleId, ::onReceive)
        onDispose { vehicleRepository.disconnect() }
    }

    return vehicle
}
