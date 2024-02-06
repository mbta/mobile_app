package com.mbta.tid.mbta_app.phoenix

import kotlinx.serialization.json.JsonObject

abstract class PhoenixChannel(
    private val socket: PhoenixSocket,
    private val topic: String,
    private val joinPayload: JsonObject
) {
    private val joinRef = socket.putChannel(this)

    suspend fun join(): RawPhoenixChannelMessage {
        val reply = socket.sendAsync(joinRef, topic, "phx_join", joinPayload)
        handle("phx_join", reply.payload)
        return reply
    }

    suspend fun leave(): RawPhoenixChannelMessage {
        val reply = socket.sendAsync(joinRef, topic, "phx_leave")
        handle("phx_leave", reply.payload)
        return reply
    }

    abstract suspend fun handle(event: String, payload: JsonObject)

    protected fun removeFromSocket() {
        socket.deleteChannel(joinRef)
    }
}
