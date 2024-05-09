package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.Outcome
import com.mbta.tid.mbta_app.model.PredictionsError
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
        onReceive: (Outcome<PredictionsStreamDataResponse?, PredictionsError>) -> Unit
    )

    fun disconnect()
}

class PredictionsRepository(private val socket: PhoenixSocket) :
    IPredictionsRepository, KoinComponent {

    var channel: PhoenixChannel? = null

    override fun connect(
        stopIds: List<String>,
        onReceive: (Outcome<PredictionsStreamDataResponse?, PredictionsError>) -> Unit
    ) {
        socket.attach()
        val joinPayload = PredictionsForStopsChannel.joinPayload(stopIds)
        channel = socket.getChannel(PredictionsForStopsChannel.topic, joinPayload)

        channel?.onEvent(PredictionsForStopsChannel.newDataEvent) { message ->
            handleNewDataMessage(message, onReceive)
        }
        channel?.onFailure { onReceive(Outcome(null, PredictionsError.Unknown)) }

        channel?.onDetach { message -> println("leaving channel ${message.subject}") }
        channel
            ?.attach()
            ?.receive(PhoenixPushStatus.Ok) { message ->
                println("joined channel ${message.subject}")
                handleNewDataMessage(message, onReceive)
            }
            ?.receive(PhoenixPushStatus.Error) {
                onReceive(Outcome(null, PredictionsError.Connection))
            }
    }

    override fun disconnect() {
        channel?.detach()
        channel = null
    }

    private fun handleNewDataMessage(
        message: PhoenixMessage,
        onReceive: (Outcome<PredictionsStreamDataResponse?, PredictionsError>) -> Unit
    ) {
        val rawPayload: String? = message.jsonBody

        if (rawPayload != null) {
            val newPredictions =
                try {
                    PredictionsForStopsChannel.parseMessage(rawPayload)
                } catch (e: IllegalArgumentException) {
                    onReceive(Outcome(null, PredictionsError.Unknown))
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
        onReceive: (Outcome<PredictionsStreamDataResponse?, PredictionsError>) -> Unit
    ) {
        /* no-op */
    }

    override fun disconnect() {
        /* no-op */
    }
}
