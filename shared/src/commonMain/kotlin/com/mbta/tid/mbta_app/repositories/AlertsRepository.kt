package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.AlertsChannel
import com.mbta.tid.mbta_app.phoenix.ChannelOwner
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.component.KoinComponent

public interface IAlertsRepository {
    public fun connect(onReceive: (ApiResult<AlertsStreamDataResponse>) -> Unit)

    public fun disconnect()
}

internal class AlertsRepository(
    socket: PhoenixSocket,
    debugRepository: IDebugRepository,
    errorBannerStateRepository: IErrorBannerStateRepository,
    ioDispatcher: CoroutineDispatcher,
) : IAlertsRepository, KoinComponent {
    private val channelOwner =
        ChannelOwner<AlertsStreamDataResponse>(
            socket,
            ioDispatcher,
            debugRepository,
            errorBannerStateRepository,
        )
    internal var channel: PhoenixChannel? by channelOwner::channel

    override fun connect(onReceive: (ApiResult<AlertsStreamDataResponse>) -> Unit) {
        channelOwner.connect(
            AlertsChannel,
            AlertsChannel::parseMessage,
            {
                when (it) {
                    is ApiResult.Ok -> println("Received ${it.data.alerts.size} alerts")
                    else -> {}
                }
                onReceive(it)
            },
            ErrorKey(KeyType.Permanent, "AlertsRepository"),
        )
    }

    override fun disconnect() {
        channelOwner.disconnect()
    }
}

public class MockAlertsRepository
@DefaultArgumentInterop.Enabled
internal constructor(
    private val result: ApiResult<AlertsStreamDataResponse>,
    private val onConnect: () -> Unit = {},
    private val onDisconnect: () -> Unit = {},
) : IAlertsRepository {
    @DefaultArgumentInterop.Enabled
    public constructor(
        response: AlertsStreamDataResponse = AlertsStreamDataResponse(emptyMap()),
        onConnect: () -> Unit = {},
        onDisconnect: () -> Unit = {},
    ) : this(ApiResult.Ok(response), onConnect, onDisconnect)

    private var receiveCallback: ((ApiResult<AlertsStreamDataResponse>) -> Unit)? = null

    override fun connect(onReceive: (ApiResult<AlertsStreamDataResponse>) -> Unit) {
        receiveCallback = onReceive
        onConnect()
        onReceive(result)
    }

    override fun disconnect() {
        onDisconnect()
    }

    internal fun receiveResult(result: ApiResult<AlertsStreamDataResponse>) {
        receiveCallback?.let { it(result) }
    }
}
