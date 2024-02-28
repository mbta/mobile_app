package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.phoenix.PhoenixChannel
import com.mbta.tid.mbta_app.phoenix.PhoenixSocket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addAll
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.putJsonArray

@OptIn(ExperimentalSerializationApi::class)
class PredictionsStopsChannel(socket: PhoenixSocket, stopIds: List<String>) :
    PhoenixChannel(
        socket,
        "predictions:stops",
        buildJsonObject { putJsonArray("stop_ids") { addAll(stopIds) } }
    ) {
    private var _earlyPredictionsChannel: Channel<PredictionsStreamDataResponse> =
        Channel(capacity = Channel.UNLIMITED)
    private var _predictionsChannel: SendChannel<PredictionsStreamDataResponse>? = null
    var predictions = channelFlow {
        _predictionsChannel = channel
        _earlyPredictionsChannel.consumeEach { channel.send(it) }
        awaitClose()
    }

    override suspend fun handle(event: String, payload: JsonObject) {
        when (event) {
            "phx_join" -> {}
            "stream_data" -> {
                val predictions: PredictionsStreamDataResponse = json.decodeFromJsonElement(payload)
                (_predictionsChannel ?: _earlyPredictionsChannel).send(predictions)
            }
            "phx_leave",
            "phx_close" -> {
                _earlyPredictionsChannel.close()
                _predictionsChannel?.close()
                removeFromSocket()
            }
            else -> throw IllegalArgumentException("Unhandled predictions channel event $event")
        }
    }
}
