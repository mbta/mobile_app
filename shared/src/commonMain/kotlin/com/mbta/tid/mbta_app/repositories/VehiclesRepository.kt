package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.greenRoutes
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.ChannelOwner
import com.mbta.tid.mbta_app.phoenix.VehiclesOnRouteChannel
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.component.KoinComponent

public interface IVehiclesRepository {
    public fun connect(
        routeId: LineOrRoute.Id,
        directionId: Int,
        errorKey: String,
        onReceive: (ApiResult<VehiclesStreamDataResponse>) -> Unit,
    )

    public fun disconnect()
}

internal class VehiclesRepository(
    socket: PhoenixSocket,
    errorBannerStateRepository: IErrorBannerStateRepository,
    ioDispatcher: CoroutineDispatcher,
) : IVehiclesRepository, KoinComponent {
    var channelOwner =
        ChannelOwner<VehiclesStreamDataResponse>(socket, ioDispatcher, errorBannerStateRepository)
    internal var channel: PhoenixChannel? by channelOwner::channel

    override fun connect(
        routeId: LineOrRoute.Id,
        directionId: Int,
        errorKey: String,
        onReceive: (ApiResult<VehiclesStreamDataResponse>) -> Unit,
    ) {
        channelOwner.connect(
            VehiclesOnRouteChannel(
                when (routeId) {
                    is Route.Id -> listOf(routeId)
                    Line.Id("line-Green") -> greenRoutes.toList()
                    is Line.Id -> listOf(Route.Id(routeId.idText))
                },
                directionId,
            ),
            VehiclesOnRouteChannel::parseMessage,
            handleResult = {
                when (it) {
                    is ApiResult.Ok -> println("Received ${it.data.vehicles.size} vehicles")
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

public class MockVehiclesRepository
@DefaultArgumentInterop.Enabled
constructor(
    internal val response: VehiclesStreamDataResponse,
    internal val onConnect: (routeId: LineOrRoute.Id, directionId: Int) -> Unit =
        { _: LineOrRoute.Id, _: Int ->
        },
    internal val onDisconnect: () -> Unit = {},
) : IVehiclesRepository {
    public constructor(
        vehicles: List<Vehicle> = emptyList()
    ) : this(response = VehiclesStreamDataResponse(vehicles.associateBy { it.id }))

    internal constructor(
        objects: ObjectCollectionBuilder
    ) : this(response = VehiclesStreamDataResponse(objects.vehicles.toMap()))

    override fun connect(
        routeId: LineOrRoute.Id,
        directionId: Int,
        errorKey: String,
        onReceive: (ApiResult<VehiclesStreamDataResponse>) -> Unit,
    ) {
        onConnect(routeId, directionId)
        onReceive(ApiResult.Ok(response))
    }

    override fun disconnect() {
        onDisconnect()
    }
}
