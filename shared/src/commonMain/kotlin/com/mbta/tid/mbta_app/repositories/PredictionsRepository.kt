package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.Outcome
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
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

    fun connectV2(
        stopIds: List<String>,
        onJoin: (Outcome<PredictionsByStopJoinResponse?, SocketError>) -> Unit,
        onMessage: (Outcome<PredictionsByStopMessageResponse?, SocketError>) -> Unit
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

    override fun connectV2(
        stopIds: List<String>,
        onJoin: (Outcome<PredictionsByStopJoinResponse?, SocketError>) -> Unit,
        onMessage: (Outcome<PredictionsByStopMessageResponse?, SocketError>) -> Unit,
    ) {
        disconnect()
        channel = socket.getChannel(PredictionsForStopsChannel.topicV2(stopIds), mapOf())

        channel?.onEvent(PredictionsForStopsChannel.newDataEvent) { message ->
            handleV2Message(message, onMessage)
        }
        channel?.onFailure { onMessage(Outcome(null, SocketError.Unknown)) }

        channel?.onDetach { message -> println("leaving channel ${message.subject}") }
        channel
            ?.attach()
            ?.receive(PhoenixPushStatus.Ok) { message ->
                println("joined channel ${message.subject}")
                handleV2JoinMessage(message, onJoin)
            }
            ?.receive(PhoenixPushStatus.Error) { onJoin(Outcome(null, SocketError.Connection)) }
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

    private fun handleV2JoinMessage(
        message: PhoenixMessage,
        onJoin: (Outcome<PredictionsByStopJoinResponse?, SocketError>) -> Unit
    ) {
        val rawPayload: String? = message.jsonBody

        if (rawPayload != null) {
            val newPredictionsByStop =
                try {
                    PredictionsForStopsChannel.parseV2JoinMessage(rawPayload)
                } catch (e: IllegalArgumentException) {
                    print("ERROR $e")
                    onJoin(Outcome(null, SocketError.Unknown))
                    return
                }
            println(
                "Received ${newPredictionsByStop.predictionsByStop.values.flatMap { it.values}.size} predictions"
            )
            onJoin(Outcome(newPredictionsByStop, null))
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
        onMessage: (Outcome<PredictionsByStopMessageResponse?, SocketError>) -> Unit
    ) {
        val rawPayload: String? = message.jsonBody

        if (rawPayload != null) {
            val newPredictionsForStop =
                try {
                    PredictionsForStopsChannel.parseV2Message(rawPayload)
                } catch (e: IllegalArgumentException) {
                    onMessage(Outcome(null, SocketError.Unknown))
                    return
                }
            println(
                "Received ${newPredictionsForStop.predictions.size} predictions for stop ${newPredictionsForStop.stopId}"
            )
            onMessage(Outcome(newPredictionsForStop, null))
        } else {
            println("No jsonPayload found for message ${message.body}")
        }
    }
}

class MockPredictionsRepository(
    val onConnect: () -> Unit = {},
    val onConnectV2: () -> Unit = {},
    val onDisconnect: () -> Unit = {},
    private val connectOutcome: Outcome<PredictionsStreamDataResponse?, SocketError>? = null,
    private val connectV2Outcome: Outcome<PredictionsByStopJoinResponse?, SocketError>? = null
) : IPredictionsRepository {

    constructor() :
        this(onConnect = {}, onConnectV2 = {}, connectOutcome = null, connectV2Outcome = null)

    constructor(
        response: PredictionsStreamDataResponse?
    ) : this(connectOutcome = Outcome(response, null))

    constructor(
        onConnect: () -> Unit = {},
        onConnectV2: () -> Unit = {},
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
        onReceive: (Outcome<PredictionsStreamDataResponse?, SocketError>) -> Unit
    ) {
        onConnect()
        if (connectOutcome != null) {
            onReceive(connectOutcome)
        }
    }

    override fun connectV2(
        stopIds: List<String>,
        onJoin: (Outcome<PredictionsByStopJoinResponse?, SocketError>) -> Unit,
        onMessage: (Outcome<PredictionsByStopMessageResponse?, SocketError>) -> Unit
    ) {
        onConnectV2()
        if (connectV2Outcome != null) {
            onJoin(connectV2Outcome)
        }
    }

    override fun disconnect() {
        onDisconnect()
    }
}
