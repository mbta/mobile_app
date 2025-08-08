package com.mbta.tid.mbta_app.repositories

import VehicleChannel
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.VehicleStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixPushStatus
import com.mbta.tid.mbta_app.network.PhoenixSocket
import org.koin.core.component.KoinComponent

public interface IVehicleRepository {
    public fun connect(vehicleId: String, onReceive: (ApiResult<VehicleStreamDataResponse>) -> Unit)

    public fun disconnect()
}

internal class VehicleRepository(private val socket: PhoenixSocket) :
    IVehicleRepository, KoinComponent {
    var channel: PhoenixChannel? = null

    override fun connect(
        vehicleId: String,
        onReceive: (ApiResult<VehicleStreamDataResponse>) -> Unit,
    ) {
        disconnect()
        channel = socket.getChannel(VehicleChannel.topic(vehicleId), emptyMap())

        channel?.onEvent(VehicleChannel.newDataEvent) { message ->
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
        onReceive: (ApiResult<VehicleStreamDataResponse>) -> Unit,
    ) {
        val rawPayload: String? = message.jsonBody

        if (rawPayload != null) {
            val newVehicleData =
                try {
                    VehicleChannel.parseMessage(rawPayload)
                } catch (e: IllegalArgumentException) {
                    onReceive(ApiResult.Error(message = SocketError.FAILED_TO_PARSE))
                    return
                }
            println("Received vehicle update")
            onReceive(ApiResult.Ok(newVehicleData))
        } else {
            println("No jsonPayload found for message ${message.body}")
        }
    }
}

public class MockVehicleRepository
@DefaultArgumentInterop.Enabled
constructor(
    internal var onConnect: () -> Unit = {},
    internal var onDisconnect: () -> Unit = {},
    private val outcome: ApiResult<VehicleStreamDataResponse>? = null,
) : IVehicleRepository {
    override fun connect(
        vehicleId: String,
        onReceive: (ApiResult<VehicleStreamDataResponse>) -> Unit,
    ) {
        outcome?.let { onReceive(it) }
        onConnect()
    }

    override fun disconnect() {
        onDisconnect()
    }
}
