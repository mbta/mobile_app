package com.mbta.tid.mbta_app.phoenix

import io.ktor.websocket.CloseReason
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MockWebSocketSession internal constructor() : WebSocketSession {
    internal val serverCloseReason: CompletableDeferred<CloseReason?> = CompletableDeferred()
    override val closeReason: Deferred<CloseReason?> = serverCloseReason

    internal val clientCloseReason: CompletableDeferred<CloseReason?> = CompletableDeferred()

    internal val outbox = Channel<RawPhoenixChannelMessage>()
    internal val inbox = Channel<RawPhoenixChannelMessage>()

    override suspend fun sendSerialized(message: RawPhoenixChannelMessage) {
        outbox.send(message)
    }

    override suspend fun close() {
        clientCloseReason.complete(CloseReason(CloseReason.Codes.NORMAL, ""))
    }

    override suspend fun consumeEachIncoming(action: suspend (RawPhoenixChannelMessage) -> Unit) {
        inbox.consumeEach { action(it) }
    }
}

class MockWebSocketSessionBuilder {
    internal val session = MockWebSocketSession()
    internal var lastSent: RawPhoenixChannelMessage? = null

    suspend fun expectSend(expected: RawPhoenixChannelMessage) {
        assertEquals(expected, session.outbox.receive())
        lastSent = expected
        delay(1)
    }

    suspend fun expectSend(
        joinRef: String? = null,
        ref: String? = null,
        topic: String,
        event: String,
        payload: JsonObject = JsonObject(emptyMap())
    ) = expectSend(RawPhoenixChannelMessage(joinRef, ref, topic, event, payload))

    suspend fun fireReceive(received: RawPhoenixChannelMessage) {
        session.inbox.send(received)
        delay(1)
    }

    suspend fun fireReceive(
        joinRef: String? = null,
        ref: String? = null,
        topic: String,
        event: String,
        payload: JsonObject = JsonObject(emptyMap())
    ) = fireReceive(RawPhoenixChannelMessage(joinRef, ref, topic, event, payload))

    suspend fun reply(status: String = "ok", response: JsonObject = JsonObject(emptyMap())) {
        val lastSent = requireNotNull(lastSent)
        fireReceive(
            joinRef = lastSent.joinRef,
            ref = lastSent.ref,
            topic = lastSent.topic,
            event = "phx_reply",
            payload =
                buildJsonObject {
                    put("status", status)
                    put("response", response)
                }
        )
    }

    fun serverClose(reason: CloseReason = CloseReason(CloseReason.Codes.NORMAL, "")) {
        session.serverCloseReason.complete(reason)
    }

    suspend fun expectClientClose(reason: CloseReason = CloseReason(CloseReason.Codes.NORMAL, "")) {
        assertEquals(reason, session.clientCloseReason.await())
    }
}

suspend fun MockWebSocketSession(
    scope: CoroutineScope,
    block: suspend MockWebSocketSessionBuilder.() -> Unit
): MockWebSocketSession {
    val builder = MockWebSocketSessionBuilder()
    scope.launch {
        builder.block()
        if (!builder.session.serverCloseReason.isCompleted) {
            builder.session.serverCloseReason.complete(CloseReason(CloseReason.Codes.NORMAL, ""))
        }
    }
    return builder.session
}
