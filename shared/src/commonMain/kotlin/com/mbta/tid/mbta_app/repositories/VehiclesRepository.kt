package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.greenRoutes
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixPushStatus
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.VehiclesOnRouteChannel
import org.koin.core.component.KoinComponent

interface IVehiclesRepository {
    fun connect(
        routeId: String,
        directionId: Int,
        onReceive: (ApiResult<VehiclesStreamDataResponse>) -> Unit
    )

    fun disconnect()
}

class VehiclesRepository(private val socket: PhoenixSocket) : IVehiclesRepository, KoinComponent {
    var channel: PhoenixChannel? = null

    override fun connect(
        routeId: String,
        directionId: Int,
        onReceive: (ApiResult<VehiclesStreamDataResponse>) -> Unit
    ) {
        val topic =
            VehiclesOnRouteChannel.topic(
                if (routeId == "line-Green") {
                    greenRoutes.toList()
                } else {
                    listOf(routeId)
                },
                directionId
            )
        channel = socket.getChannel(topic, emptyMap())

        channel?.onEvent(VehiclesOnRouteChannel.newDataEvent) { message ->
            handleNewDataMessage(message, onReceive)
        }
        channel?.onFailure { onReceive(ApiResult.Error(message = SocketError.FAILURE)) }

        channel?.onDetach { message -> println("leaving channel ${message.subject}") }
        channel
            ?.attach()
            ?.receive(PhoenixPushStatus.Ok) { message ->
                println("joined channel ${message.subject}")
                handleNewDataMessage(message, onReceive)
            }
            ?.receive(PhoenixPushStatus.Error) {
                onReceive(ApiResult.Error(message = SocketError.RECEIVED_ERROR))
            }
    }

    override fun disconnect() {
        channel?.detach()
        channel = null
    }

    private fun handleNewDataMessage(
        message: PhoenixMessage,
        onReceive: (ApiResult<VehiclesStreamDataResponse>) -> Unit
    ) {
        val rawPayload: String? = message.jsonBody

        if (rawPayload != null) {
            val newVehicleData: VehiclesStreamDataResponse =
                try {
                    VehiclesOnRouteChannel.parseMessage(rawPayload)
                } catch (e: IllegalArgumentException) {
                    onReceive(ApiResult.Error(message = SocketError.FAILED_TO_PARSE))
                    return
                }
            println("Received ${newVehicleData.vehicles.size} vehicles")
            onReceive(ApiResult.Ok(newVehicleData))
        } else {
            println("No jsonPayload found for message ${message.body}")
        }
    }
}

class MockVehiclesRepository(val vehicles: List<Vehicle> = emptyList()) : IVehiclesRepository {
    override fun connect(
        routeId: String,
        directionId: Int,
        onReceive: (ApiResult<VehiclesStreamDataResponse>) -> Unit
    ) {
        onReceive(ApiResult.Ok(VehiclesStreamDataResponse(vehicles.associateBy { it.id })))
    }

    override fun disconnect() {
        /* no-op */
    }
}
