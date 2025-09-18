package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.greenRoutes
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.ChannelOwner
import com.mbta.tid.mbta_app.phoenix.VehiclesOnRouteChannel
import org.koin.core.component.KoinComponent

public interface IVehiclesRepository {
    public fun connect(
        routeId: String,
        directionId: Int,
        onReceive: (ApiResult<VehiclesStreamDataResponse>) -> Unit,
    )

    public fun disconnect()
}

internal class VehiclesRepository(socket: PhoenixSocket) : IVehiclesRepository, KoinComponent {
    var channelOwner = ChannelOwner(socket)
    internal var channel: PhoenixChannel? by channelOwner::channel

    override fun connect(
        routeId: String,
        directionId: Int,
        onReceive: (ApiResult<VehiclesStreamDataResponse>) -> Unit,
    ) {
        channelOwner.connect(
            VehiclesOnRouteChannel(
                if (routeId == "line-Green") greenRoutes.toList() else listOf(routeId),
                directionId,
            ),
            handleMessage = { handleNewDataMessage(it, onReceive) },
            handleError = { onReceive(ApiResult.Error(message = it)) },
        )
    }

    override fun disconnect() {
        channelOwner.disconnect()
    }

    private fun handleNewDataMessage(
        message: PhoenixMessage,
        onReceive: (ApiResult<VehiclesStreamDataResponse>) -> Unit,
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

public class MockVehiclesRepository
@DefaultArgumentInterop.Enabled
constructor(
    internal val response: VehiclesStreamDataResponse,
    internal val onConnect: (routeId: String, directionId: Int) -> Unit = { _: String, _: Int -> },
    internal val onDisconnect: () -> Unit = {},
) : IVehiclesRepository {
    public constructor(
        vehicles: List<Vehicle> = emptyList()
    ) : this(response = VehiclesStreamDataResponse(vehicles.associateBy { it.id }))

    internal constructor(
        objects: ObjectCollectionBuilder
    ) : this(response = VehiclesStreamDataResponse(objects.vehicles))

    override fun connect(
        routeId: String,
        directionId: Int,
        onReceive: (ApiResult<VehiclesStreamDataResponse>) -> Unit,
    ) {
        onConnect(routeId, directionId)
        onReceive(ApiResult.Ok(response))
    }

    override fun disconnect() {
        onDisconnect()
    }
}
