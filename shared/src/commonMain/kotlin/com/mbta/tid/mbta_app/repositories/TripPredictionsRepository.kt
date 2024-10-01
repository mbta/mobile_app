package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixPushStatus
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.PredictionsForStopsChannel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent

interface ITripPredictionsRepository {
    fun connect(tripId: String, onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit)

    var lastUpdated: Instant?

    fun disconnect()
}

class TripPredictionsRepository(private val socket: PhoenixSocket) :
    ITripPredictionsRepository, KoinComponent {

    var channel: PhoenixChannel? = null

    override var lastUpdated: Instant? = null

    override fun connect(
        tripId: String,
        onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit
    ) {
        channel = socket.getChannel("predictions:trip:$tripId", emptyMap())

        channel?.onEvent(PredictionsForStopsChannel.newDataEvent) { message ->
            handleNewDataMessage(message, onReceive)
        }
        channel?.onFailure { onReceive(ApiResult.Error(message = SocketError.FAILURE)) }

        channel?.onDetach { message -> println("leaving channel ${message.subject}") }
        channel
            ?.attach()
            ?.receive(PhoenixPushStatus.Ok) { message ->
                println("joined channel ${message.subject}")
                handleNewDataMessage(message, onReceive)
            }
            ?.receive(PhoenixPushStatus.Error) {
                onReceive(ApiResult.Error(message = SocketError.RECEIVED_ERROR))
            }
    }

    override fun disconnect() {
        channel?.detach()
        channel = null
    }

    private fun handleNewDataMessage(
        message: PhoenixMessage,
        onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit
    ) {
        val rawPayload: String? = message.jsonBody

        if (rawPayload != null) {
            val newPredictions =
                try {
                    PredictionsForStopsChannel.parseMessage(rawPayload)
                } catch (e: IllegalArgumentException) {
                    onReceive(ApiResult.Error(message = SocketError.FAILED_TO_PARSE))
                    return
                }
            println("Received ${newPredictions.predictions.size} predictions")
            lastUpdated = Clock.System.now()
            onReceive(ApiResult.Ok(newPredictions))
        } else {
            println("No jsonPayload found for message ${message.body}")
        }
    }
}

class MockTripPredictionsRepository : ITripPredictionsRepository {

    override fun connect(
        tripId: String,
        onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit
    ) {
        /* no-op */
    }

    override var lastUpdated: Instant? = null

    override fun disconnect() {
        /* no-op */
    }
}
