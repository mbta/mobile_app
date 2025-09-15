package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.network.receiveAll
import com.mbta.tid.mbta_app.phoenix.AlertsChannel
import org.koin.core.component.KoinComponent

public interface IAlertsRepository {
    public fun connect(onReceive: (ApiResult<AlertsStreamDataResponse>) -> Unit)

    public fun disconnect()
}

internal class AlertsRepository(private val socket: PhoenixSocket) :
    IAlertsRepository, KoinComponent {

    var channel: PhoenixChannel? = null

    override fun connect(onReceive: (ApiResult<AlertsStreamDataResponse>) -> Unit) {
        disconnect()
        channel = socket.getChannel(AlertsChannel.topic, emptyMap())

        channel?.onEvent(AlertsChannel.newDataEvent) { message ->
            handleNewDataMessage(message, onReceive)
        }
        channel?.onFailure { onReceive(ApiResult.Error(message = SocketError.FAILURE)) }

        channel?.onDetach { message -> println("leaving channel ${message.subject}") }
        channel
            ?.attach()
            ?.receiveAll(
                onOk = { message ->
                    println("joined channel ${message.subject}")
                    handleNewDataMessage(message, onReceive)
                },
                onError = { onReceive(ApiResult.Error(message = SocketError.RECEIVED_ERROR)) },
                onTimeout = { onReceive(ApiResult.Error(message = SocketError.TIMEOUT)) },
            )
    }

    override fun disconnect() {
        channel?.detach()
        channel = null
    }

    private fun handleNewDataMessage(
        message: PhoenixMessage,
        onReceive: (ApiResult<AlertsStreamDataResponse>) -> Unit,
    ) {
        val rawPayload = message.jsonBody
        if (rawPayload != null) {
            val newAlerts =
                try {
                    AlertsChannel.parseMessage(rawPayload)
                } catch (e: IllegalArgumentException) {
                    onReceive(ApiResult.Error(message = SocketError.FAILED_TO_PARSE))
                    println("${e.message}")
                    return
                }
            println("Received ${newAlerts.alerts.size} alerts")
            onReceive(ApiResult.Ok(newAlerts))
        } else {
            println("No jsonPayload found for message ${message.body}")
        }
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
