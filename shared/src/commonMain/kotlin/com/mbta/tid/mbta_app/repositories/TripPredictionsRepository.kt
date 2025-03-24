package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixPushStatus
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.PredictionsForStopsChannel
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent

interface ITripPredictionsRepository {
    fun connect(tripId: String, onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit)

    var lastUpdated: Instant?

    fun shouldForgetPredictions(predictionCount: Int): Boolean

    fun disconnect()
}

class TripPredictionsRepository(private val socket: PhoenixSocket) :
    ITripPredictionsRepository, KoinComponent {

    var channel: PhoenixChannel? = null

    override var lastUpdated: Instant? = null

    override fun shouldForgetPredictions(predictionCount: Int) =
        (Clock.System.now() - (lastUpdated ?: Instant.DISTANT_FUTURE)) > 10.minutes &&
            predictionCount > 0

    override fun connect(
        tripId: String,
        onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit
    ) {
        disconnect()
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

class MockTripPredictionsRepository
@DefaultArgumentInterop.Enabled
constructor(
    var onConnect: () -> Unit = {},
    var onDisconnect: () -> Unit = {},
    var response: PredictionsStreamDataResponse =
        PredictionsStreamDataResponse(emptyMap(), emptyMap(), emptyMap())
) : ITripPredictionsRepository {

    override fun connect(
        tripId: String,
        onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit
    ) {
        onReceive(ApiResult.Ok(response))
        onConnect()
    }

    override var lastUpdated: Instant? = null

    override fun shouldForgetPredictions(predictionCount: Int) = false

    override fun disconnect() {
        onDisconnect()
    }
}
