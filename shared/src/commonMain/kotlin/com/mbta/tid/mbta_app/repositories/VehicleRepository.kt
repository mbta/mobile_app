package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.VehicleStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.ChannelOwner
import com.mbta.tid.mbta_app.phoenix.VehicleChannel
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.component.KoinComponent

public interface IVehicleRepository {
    public fun connect(
        vehicleId: String,
        errorKey: String,
        onReceive: (ApiResult<VehicleStreamDataResponse>) -> Unit,
    )

    public fun disconnect()
}

internal class VehicleRepository(
    socket: PhoenixSocket,
    debugRepository: IDebugRepository,
    errorBannerStateRepository: IErrorBannerStateRepository,
    ioDispatcher: CoroutineDispatcher,
) : IVehicleRepository, KoinComponent {
    private val channelOwner =
        ChannelOwner<VehicleStreamDataResponse>(
            socket,
            ioDispatcher,
            debugRepository,
            errorBannerStateRepository,
        )
    internal var channel: PhoenixChannel? by channelOwner::channel

    override fun connect(
        vehicleId: String,
        errorKey: String,
        onReceive: (ApiResult<VehicleStreamDataResponse>) -> Unit,
    ) {
        channelOwner.connect(
            VehicleChannel(vehicleId),
            parseMessage = VehicleChannel::parseMessage,
            handleResult = {
                when (it) {
                    is ApiResult.Ok -> println("Received vehicle update")
                    else -> {}
                }
                onReceive(it)
            },
            errorKey = errorKey,
        )
    }

    override fun disconnect() {
        channelOwner.disconnect()
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
        errorKey: String,
        onReceive: (ApiResult<VehicleStreamDataResponse>) -> Unit,
    ) {
        outcome?.let { onReceive(it) }
        onConnect()
    }

    override fun disconnect() {
        onDisconnect()
    }
}
