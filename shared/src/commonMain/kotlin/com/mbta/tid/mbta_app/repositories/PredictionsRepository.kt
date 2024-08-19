package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.Outcome
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixPushStatus
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.PredictionsForStopsChannel
import org.koin.core.component.KoinComponent

interface IPredictionsRepository {
    fun connect(
        stopIds: List<String>,
        onReceive: (Outcome<PredictionsStreamDataResponse?, SocketError>) -> Unit
    )

    fun disconnect()
}

class PredictionsRepository(private val socket: PhoenixSocket) :
    IPredictionsRepository, KoinComponent {

    var channel: PhoenixChannel? = null

    override fun connect(
        stopIds: List<String>,
        onReceive: (Outcome<PredictionsStreamDataResponse?, SocketError>) -> Unit
    ) {
        val joinPayload = PredictionsForStopsChannel.joinPayload(stopIds)
        channel = socket.getChannel(PredictionsForStopsChannel.topic, joinPayload)

        channel?.onEvent(PredictionsForStopsChannel.newDataEvent) { message ->
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
        onReceive: (Outcome<PredictionsStreamDataResponse?, SocketError>) -> Unit
    ) {
        val rawPayload: String? = message.jsonBody

        if (rawPayload != null) {
            val newPredictions =
                try {
                    PredictionsForStopsChannel.parseMessage(rawPayload)
                } catch (e: IllegalArgumentException) {
                    onReceive(Outcome(null, SocketError.Unknown))
                    return
                }
            println("Received ${newPredictions.predictions.size} predictions")
            onReceive(Outcome(newPredictions, null))
        } else {
            println("No jsonPayload found for message ${message.body}")
        }
    }
}

class MockPredictionsRepository : IPredictionsRepository {

    override fun connect(
        stopIds: List<String>,
        onReceive: (Outcome<PredictionsStreamDataResponse?, SocketError>) -> Unit
    ) {
        /* no-op */
    }

    override fun disconnect() {
        /* no-op */
    }
}
