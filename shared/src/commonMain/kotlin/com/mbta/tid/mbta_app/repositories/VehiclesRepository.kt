package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.Outcome
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.Vehicle
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
        onReceive: (Outcome<VehiclesStreamDataResponse?, SocketError>) -> Unit
    )

    fun disconnect()
}

class VehiclesRepository(private val socket: PhoenixSocket) : IVehiclesRepository, KoinComponent {
    var channel: PhoenixChannel? = null

    override fun connect(
        routeId: String,
        directionId: Int,
        onReceive: (Outcome<VehiclesStreamDataResponse?, SocketError>) -> Unit
    ) {
        socket.attach()
        val joinPayload = VehiclesOnRouteChannel.joinPayload(routeId, directionId)
        channel = socket.getChannel(VehiclesOnRouteChannel.topic, joinPayload)

        channel?.onEvent(VehiclesOnRouteChannel.newDataEvent) { message ->
            handleNewDataMessage(message, onReceive)
        }
        channel?.onFailure { onReceive(Outcome(null, SocketError.Unknown)) }

        channel?.onDetach { message -> println("leaving channel ${message.subject}") }
        channel
            ?.attach()
            ?.receive(PhoenixPushStatus.Ok) { message ->
                println("joined channel ${message.subject}")
                handleNewDataMessage(message, onReceive)
            }
            ?.receive(PhoenixPushStatus.Error) { onReceive(Outcome(null, SocketError.Connection)) }
    }

    override fun disconnect() {
        channel?.detach()
        channel = null
    }

    private fun handleNewDataMessage(
        message: PhoenixMessage,
        onReceive: (Outcome<VehiclesStreamDataResponse?, SocketError>) -> Unit
    ) {
        val rawPayload: String? = message.jsonBody

        if (rawPayload != null) {
            val newVehicleData: VehiclesStreamDataResponse =
                try {
                    VehiclesOnRouteChannel.parseMessage(rawPayload)
                } catch (e: IllegalArgumentException) {
                    onReceive(Outcome(null, SocketError.Unknown))
                    return
                }
            println("Received ${newVehicleData.vehicles.size} vehicles")
            onReceive(Outcome(newVehicleData, null))
        } else {
            println("No jsonPayload found for message ${message.body}")
        }
    }
}

class MockVehiclesRepository(val vehicles: List<Vehicle> = emptyList()) : IVehiclesRepository {
    override fun connect(
        routeId: String,
        directionId: Int,
        onReceive: (Outcome<VehiclesStreamDataResponse?, SocketError>) -> Unit
    ) {
        onReceive(Outcome(VehiclesStreamDataResponse(vehicles.associateBy { it.id }), null))
    }

    override fun disconnect() {
        /* no-op */
    }
}