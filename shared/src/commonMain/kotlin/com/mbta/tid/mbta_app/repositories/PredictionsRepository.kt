package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
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

interface IPredictionsRepository {
    fun connect(
        stopIds: List<String>,
        onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit
    )

    fun connectV2(
        stopIds: List<String>,
        onJoin: (ApiResult<PredictionsByStopJoinResponse>) -> Unit,
        onMessage: (ApiResult<PredictionsByStopMessageResponse>) -> Unit
    )

    var lastUpdated: Instant?

    fun shouldForgetPredictions(predictionCount: Int): Boolean

    fun disconnect()
}

class PredictionsRepository(private val socket: PhoenixSocket) :
    IPredictionsRepository, KoinComponent {

    var channel: PhoenixChannel? = null

    override var lastUpdated: Instant? = null

    override fun shouldForgetPredictions(predictionCount: Int) =
        (Clock.System.now() - (lastUpdated ?: Instant.DISTANT_FUTURE)) > 10.minutes &&
            predictionCount > 0

    override fun connect(
        stopIds: List<String>,
        onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit
    ) {
        val joinPayload = PredictionsForStopsChannel.joinPayload(stopIds)
        channel = socket.getChannel(PredictionsForStopsChannel.topic, joinPayload)

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

    override fun connectV2(
        stopIds: List<String>,
        onJoin: (ApiResult<PredictionsByStopJoinResponse>) -> Unit,
        onMessage: (ApiResult<PredictionsByStopMessageResponse>) -> Unit,
    ) {
        disconnect()
        channel = socket.getChannel(PredictionsForStopsChannel.topicV2(stopIds), mapOf())

        channel?.onEvent(PredictionsForStopsChannel.newDataEvent) { message ->
            handleV2Message(message, onMessage)
        }
        channel?.onFailure { onMessage(ApiResult.Error(message = SocketError.FAILURE)) }

        channel?.onDetach { message -> println("leaving channel ${message.subject}") }
        channel
            ?.attach()
            ?.receive(PhoenixPushStatus.Ok) { message ->
                println("joined channel ${message.subject}")
                handleV2JoinMessage(message, onJoin)
            }
            ?.receive(PhoenixPushStatus.Error) {
                onJoin(ApiResult.Error(message = SocketError.RECEIVED_ERROR))
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

    private fun handleV2JoinMessage(
        message: PhoenixMessage,
        onJoin: (ApiResult<PredictionsByStopJoinResponse>) -> Unit
    ) {
        val rawPayload: String? = message.jsonBody

        if (rawPayload != null) {
            val newPredictionsByStop =
                try {
                    PredictionsForStopsChannel.parseV2JoinMessage(rawPayload)
                } catch (e: IllegalArgumentException) {
                    print("ERROR $e")
                    onJoin(ApiResult.Error(message = SocketError.FAILED_TO_PARSE))
                    return
                }
            println(
                "Received ${newPredictionsByStop.predictionsByStop.values.flatMap { it.values}.size} predictions on join"
            )
            lastUpdated = Clock.System.now()
            onJoin(ApiResult.Ok(newPredictionsByStop))
        } else {
            println("No jsonPayload found for message ${message.body}")
        }
    }

    /**
     * Parse the phoenix message & pass to the onMessage callback
     *
     * @param message: the message to parse, expected as a PredictionsByStopMessageResponse
     * @param onMessage: the callback ot invoke on the parsed message
     */
    internal fun handleV2Message(
        message: PhoenixMessage,
        onMessage: (ApiResult<PredictionsByStopMessageResponse>) -> Unit
    ) {
        val rawPayload: String? = message.jsonBody

        if (rawPayload != null) {
            val newPredictionsForStop =
                try {
                    PredictionsForStopsChannel.parseV2Message(rawPayload)
                } catch (e: IllegalArgumentException) {
                    onMessage(ApiResult.Error(message = SocketError.FAILED_TO_PARSE))
                    return
                }
            lastUpdated = Clock.System.now()
            onMessage(ApiResult.Ok(newPredictionsForStop))
        } else {
            println("No jsonPayload found for message ${message.body}")
        }
    }
}

class MockPredictionsRepository(
    val onConnect: () -> Unit = {},
    val onConnectV2: (List<String>) -> Unit = {},
    val onDisconnect: () -> Unit = {},
    private val connectOutcome: ApiResult<PredictionsStreamDataResponse>? = null,
    private val connectV2Outcome: ApiResult<PredictionsByStopJoinResponse>? = null
) : IPredictionsRepository {

    constructor() :
        this(onConnect = {}, onConnectV2 = {}, connectOutcome = null, connectV2Outcome = null)

    constructor(
        response: PredictionsStreamDataResponse
    ) : this(connectOutcome = ApiResult.Ok(response))

    constructor(
        connectV2Outcome: PredictionsByStopJoinResponse
    ) : this(connectV2Outcome = ApiResult.Ok(connectV2Outcome))

    constructor(
        onConnect: () -> Unit = {},
        onConnectV2: (List<String>) -> Unit = {},
        onDisconnect: () -> Unit = {},
    ) : this(
        onConnect = onConnect,
        onConnectV2 = onConnectV2,
        onDisconnect = onDisconnect,
        connectOutcome = null,
        connectV2Outcome = null
    )

    override fun connect(
        stopIds: List<String>,
        onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit
    ) {
        onConnect()
        if (connectOutcome != null) {
            onReceive(connectOutcome)
        }
    }

    override fun connectV2(
        stopIds: List<String>,
        onJoin: (ApiResult<PredictionsByStopJoinResponse>) -> Unit,
        onMessage: (ApiResult<PredictionsByStopMessageResponse>) -> Unit
    ) {
        onConnectV2(stopIds)
        if (connectV2Outcome != null) {
            onJoin(connectV2Outcome)
        }
    }

    override var lastUpdated: Instant? = null

    override fun shouldForgetPredictions(predictionCount: Int) = false

    override fun disconnect() {
        onDisconnect()
    }
}
