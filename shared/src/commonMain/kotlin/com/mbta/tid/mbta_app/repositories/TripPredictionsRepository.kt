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

interface ITripPredictionsRepository {
    fun connect(
        tripId: String,
        onReceive: (Outcome<PredictionsStreamDataResponse?, SocketError>) -> Unit
    )

    fun disconnect()
}

class TripPredictionsRepository(private val socket: PhoenixSocket) :
    ITripPredictionsRepository, KoinComponent {

    var channel: PhoenixChannel? = null

    override fun connect(
        tripId: String,
        onReceive: (Outcome<PredictionsStreamDataResponse?, SocketError>) -> Unit
    ) {
        channel = socket.getChannel("predictions:trip:$tripId", emptyMap())

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

class MockTripPredictionsRepository : ITripPredictionsRepository {

    override fun connect(
        tripId: String,
        onReceive: (Outcome<PredictionsStreamDataResponse?, SocketError>) -> Unit
    ) {
        /* no-op */
    }

    override fun disconnect() {
        /* no-op */
    }
}
