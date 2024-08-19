package com.mbta.tid.mbta_app.repositories

import VehicleChannel
import com.mbta.tid.mbta_app.model.Outcome
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.VehicleStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixPushStatus
import com.mbta.tid.mbta_app.network.PhoenixSocket
import org.koin.core.component.KoinComponent

interface IVehicleRepository {
    fun connect(
        vehicleId: String,
        onReceive: (Outcome<VehicleStreamDataResponse?, SocketError>) -> Unit
    )

    fun disconnect()
}

class VehicleRepository(private val socket: PhoenixSocket) : IVehicleRepository, KoinComponent {
    var channel: PhoenixChannel? = null

    override fun connect(
        vehicleId: String,
        onReceive: (Outcome<VehicleStreamDataResponse?, SocketError>) -> Unit
    ) {
        channel = socket.getChannel(VehicleChannel.topic(vehicleId), emptyMap())

        channel?.onEvent(VehicleChannel.newDataEvent) { message ->
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
        onReceive: (Outcome<VehicleStreamDataResponse?, SocketError>) -> Unit
    ) {
        val rawPayload: String? = message.jsonBody

        if (rawPayload != null) {
            val newVehicleData =
                try {
                    VehicleChannel.parseMessage(rawPayload)
                } catch (e: IllegalArgumentException) {
                    onReceive(Outcome(null, SocketError.Unknown))
                    return
                }
            println("Received vehicle update")
            onReceive(Outcome(newVehicleData, null))
        } else {
            println("No jsonPayload found for message ${message.body}")
        }
    }
}

class MockVehicleRepository : IVehicleRepository {
    override fun connect(
        vehicleId: String,
        onReceive: (Outcome<VehicleStreamDataResponse?, SocketError>) -> Unit
    ) {
        /* no-op */
    }

    override fun disconnect() {
        /* no-op */
    }
}
