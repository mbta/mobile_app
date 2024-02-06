package com.mbta.tid.mbta_app.phoenix

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.converter
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.serialization.deserialize
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.consumeEach

interface WebSocketSession {
    val closeReason: Deferred<CloseReason?>

    suspend fun sendSerialized(message: RawPhoenixChannelMessage)

    suspend fun close()

    suspend fun consumeEachIncoming(action: suspend (RawPhoenixChannelMessage) -> Unit)
}

class RealWebSocketSession(private val session: DefaultClientWebSocketSession) : WebSocketSession {
    override val closeReason: Deferred<CloseReason?> = session.closeReason

    override suspend fun sendSerialized(message: RawPhoenixChannelMessage) {
        session.sendSerialized(message)
    }

    override suspend fun close() {
        session.close()
    }

    override suspend fun consumeEachIncoming(action: suspend (RawPhoenixChannelMessage) -> Unit) {
        session.incoming.consumeEach { frame ->
            val message: RawPhoenixChannelMessage = session.converter!!.deserialize(frame)
            action(message)
        }
    }
}

fun DefaultClientWebSocketSession.wrap(): WebSocketSession = RealWebSocketSession(this)
