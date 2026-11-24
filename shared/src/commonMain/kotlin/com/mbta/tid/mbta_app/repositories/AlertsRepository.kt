package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.AlertsStreamUpdateResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.AlertsChannel
import com.mbta.tid.mbta_app.phoenix.AsymmetricChannelOwner
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.component.KoinComponent

public interface IAlertsRepository {
    public fun connect(
        onJoin: (ApiResult<AlertsStreamUpdateResponse>) -> Unit,
        onReceive: (ApiResult<AlertsStreamUpdateResponse>) -> Unit,
    )

    public fun disconnect()
}

internal class AlertsRepository(
    socket: PhoenixSocket,
    debugRepository: IDebugRepository,
    errorBannerStateRepository: IErrorBannerStateRepository,
    ioDispatcher: CoroutineDispatcher,
) : IAlertsRepository, KoinComponent {
    private val channelOwner =
        AsymmetricChannelOwner<AlertsStreamUpdateResponse, AlertsStreamUpdateResponse>(
            socket,
            ioDispatcher,
            debugRepository,
            errorBannerStateRepository,
        )
    internal var channel: PhoenixChannel? by channelOwner::channel

    override fun connect(
        onJoin: (ApiResult<AlertsStreamUpdateResponse>) -> Unit,
        onReceive: (ApiResult<AlertsStreamUpdateResponse>) -> Unit,
    ) {
        channelOwner.connect(
            AlertsChannel,
            AlertsChannel::parseMessage,
            AlertsChannel::parseMessage,
            onJoin,
            {
                when (it) {
                    is ApiResult.Ok -> println("Received ${it.data.update.size} alerts")
                    else -> {}
                }
                onReceive(it)
            },
            ErrorKey(setOf(), "AlertsRepository"),
        )
    }

    override fun disconnect() {
        channelOwner.disconnect()
    }
}

public class MockAlertsRepository
@DefaultArgumentInterop.Enabled
internal constructor(
    private val result: ApiResult<AlertsStreamUpdateResponse>,
    private val onConnect: () -> Unit = {},
    private val onDisconnect: () -> Unit = {},
) : IAlertsRepository {
    @DefaultArgumentInterop.Enabled
    public constructor(
        response: AlertsStreamUpdateResponse = AlertsStreamUpdateResponse(emptyList(), emptyMap()),
        onConnect: () -> Unit = {},
        onDisconnect: () -> Unit = {},
    ) : this(ApiResult.Ok(response), onConnect, onDisconnect)

    private var receiveCallback: ((ApiResult<AlertsStreamUpdateResponse>) -> Unit)? = null
    private var joinCallback: ((ApiResult<AlertsStreamUpdateResponse>) -> Unit)? = null

    override fun connect(
        onJoin: (ApiResult<AlertsStreamUpdateResponse>) -> Unit,
        onReceive: (ApiResult<AlertsStreamUpdateResponse>) -> Unit,
    ) {
        joinCallback = onJoin
        receiveCallback = onReceive
        onConnect()
        receiveJoinResult(result)
    }

    override fun disconnect() {
        onDisconnect()
    }

    internal fun receiveResult(result: ApiResult<AlertsStreamUpdateResponse>) {
        receiveCallback?.let { it(result) }
    }

    internal fun receiveJoinResult(result: ApiResult<AlertsStreamUpdateResponse>) {
        joinCallback?.invoke(result)
    }
}
