package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.Outcome
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixPushStatus
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.AlertsChannel
import org.koin.core.component.KoinComponent

interface IAlertsRepository {
    fun connect(onReceive: (Outcome<AlertsStreamDataResponse?, SocketError>) -> Unit)

    fun disconnect()
}

class AlertsRepository(private val socket: PhoenixSocket) : IAlertsRepository, KoinComponent {

    var channel: PhoenixChannel? = null

    override fun connect(onReceive: (Outcome<AlertsStreamDataResponse?, SocketError>) -> Unit) {
        socket.attach()
        channel?.detach()
        channel = socket.getChannel(AlertsChannel.topic, emptyMap())

        channel?.onEvent(AlertsChannel.newDataEvent) { message ->
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
        onReceive: (Outcome<AlertsStreamDataResponse?, SocketError>) -> Unit
    ) {
        val rawPayload = message.jsonBody
        if (rawPayload != null) {
            val newAlerts =
                try {
                    AlertsChannel.parseMessage(rawPayload)
                } catch (e: IllegalArgumentException) {
                    onReceive(Outcome(null, SocketError.Unknown))
                    println("${e.message}")
                    return
                }
            val splitNewAlerts =
                if (splitAlert566172) {
                    newAlerts.splitAlert566172()
                } else {
                    newAlerts
                }
            println("Received ${newAlerts.alerts.size} alerts")
            onReceive(Outcome(splitNewAlerts, null))
        } else {
            println("No jsonPayload found for message ${message.body}")
        }
    }

    companion object {
        // TODO: remove after debugging mixed shuttle/suspension alerts
        const val splitAlert566172 = true
    }
}

class MockAlertsRepository : IAlertsRepository {

    override fun connect(onReceive: (Outcome<AlertsStreamDataResponse?, SocketError>) -> Unit) {
        /* no-op */
    }

    override fun disconnect() {
        /* no-op */
    }
}
