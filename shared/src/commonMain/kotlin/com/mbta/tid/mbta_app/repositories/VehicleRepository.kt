package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.VehicleStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.ChannelOwner
import com.mbta.tid.mbta_app.phoenix.VehicleChannel
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.component.KoinComponent

public interface IVehicleRepository {
    public fun connect(vehicleId: String, onReceive: (ApiResult<VehicleStreamDataResponse>) -> Unit)

    public fun disconnect()
}

internal class VehicleRepository(socket: PhoenixSocket, ioDispatcher: CoroutineDispatcher) :
    IVehicleRepository, KoinComponent {
    private val channelOwner = ChannelOwner(socket, ioDispatcher)
    internal var channel: PhoenixChannel? by channelOwner::channel

    override fun connect(
        vehicleId: String,
        onReceive: (ApiResult<VehicleStreamDataResponse>) -> Unit,
    ) {
        channelOwner.connect(
            VehicleChannel(vehicleId),
            handleMessage = { handleNewDataMessage(it, onReceive) },
            handleError = { onReceive(ApiResult.Error(message = it)) },
        )
    }

    override fun disconnect() {
        channelOwner.disconnect()
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
