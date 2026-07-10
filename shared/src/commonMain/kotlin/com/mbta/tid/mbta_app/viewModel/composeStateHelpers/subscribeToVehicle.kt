package com.mbta.tid.mbta_app.viewModel.composeStateHelpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.VehicleStreamDataResponse
import com.mbta.tid.mbta_app.repositories.ErrorKey
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IVehicleRepository
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import org.koin.compose.koinInject

public data class VehicleSubscriptionResponse(val vehicle: Vehicle?, val lastResponseStale: Boolean)

@Composable
internal fun subscribeToVehicle(
    vehicleId: String?,
    errorKey: ErrorKey,
    active: Boolean,
    errorBannerRepository: IErrorBannerStateRepository = koinInject(),
    vehicleRepository: IVehicleRepository = koinInject(),
): VehicleSubscriptionResponse {
    var vehicle: Vehicle? by remember { mutableStateOf(null) }
    var lastResponseStale: Boolean by remember { mutableStateOf(false) }
    val errorKey = errorKey.withSuffix("subscribeToVehicle")

    fun connect(vehicleId: String?, onReceive: (ApiResult<VehicleStreamDataResponse>) -> Unit) {
        if (active) vehicleId?.let { vehicleRepository.connect(it, errorKey, onReceive) }
    }

    fun onReceive(message: ApiResult<VehicleStreamDataResponse>) {
        when (message) {
            is ApiResult.Ok ->
                message.data.vehicle?.let {
                    if (it.isStale()) {
                        lastResponseStale = true
                        Sentry.captureMessage("Stale vehicle received") { scope ->
                            scope.addBreadcrumb(
                                Breadcrumb(
                                    message = "Vehicle significantly out of date",
                                    data =
                                        mutableMapOf(
                                            "id" to it.id,
                                            "updatedAt" to it.updatedAt,
                                            "routeId" to (it.routeId?.idText ?: "nil"),
                                            "tripId" to (it.tripId ?: "nil"),
                                            "stopId" to (it.stopId ?: "nil"),
                                            "currentStatus" to it.currentStatus,
                                        ),
                                )
                            )
                        }
                    } else {
                        vehicle = it
                        lastResponseStale = false
                    }
                }
            is ApiResult.Error -> {
                println("Vehicle stream failed to join: ${message.message}")
                vehicle = null
                lastResponseStale = false
            }
        }
    }

    LaunchedEffect(vehicleId, active) {
        if (active) {
            if (vehicle?.id != vehicleId) {
                vehicle = null
                lastResponseStale = false
            }
            connect(vehicleId, ::onReceive)
        } else {
            lastResponseStale = false
            vehicleRepository.disconnect()
        }
    }

    return VehicleSubscriptionResponse(vehicle, lastResponseStale)
}
