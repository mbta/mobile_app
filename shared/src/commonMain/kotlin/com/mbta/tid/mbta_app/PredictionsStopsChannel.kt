package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.phoenix.PhoenixChannel
import com.mbta.tid.mbta_app.phoenix.PhoenixSocket
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
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
    private lateinit var _predictionsChannel: SendChannel<List<Prediction>>
    var predictions = channelFlow {
        _predictionsChannel = channel
        awaitClose()
    }

    override suspend fun handle(event: String, payload: JsonObject) {
        when (event) {
            "phx_join" -> {}
            "stream_data" -> {
                val predictions: List<Prediction> =
                    Json.decodeFromJsonElement(payload["predictions"]!!)
                _predictionsChannel.send(predictions.sorted())
            }
            "phx_leave",
            "phx_close" -> {
                _predictionsChannel.close()
                removeFromSocket()
            }
            else -> throw IllegalArgumentException("Unhandled predictions channel event $event")
        }
    }
}
