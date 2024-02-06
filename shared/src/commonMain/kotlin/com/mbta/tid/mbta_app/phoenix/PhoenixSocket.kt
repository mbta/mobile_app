package com.mbta.tid.mbta_app.phoenix

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.JsonObject

class PhoenixSocket(private var conn: WebSocketSession) {
    private var nextJoinRef = 0
    private var nextRef = 0

    private var channels = mutableMapOf<String, PhoenixChannel>()
    private var messages = mutableMapOf<String, CompletableDeferred<RawPhoenixChannelMessage>>()

    internal fun putChannel(channel: PhoenixChannel): String {
        val joinRef = "$nextJoinRef"
        nextJoinRef++
        channels[joinRef] = channel
        return joinRef
    }

    internal fun deleteChannel(joinRef: String) {
        channels.remove(joinRef)
    }

    internal suspend fun sendAsync(
        joinRef: String? = null,
        topic: String,
        event: String,
        payload: JsonObject = JsonObject(emptyMap())
    ): RawPhoenixChannelMessage {
        val result = CompletableDeferred<RawPhoenixChannelMessage>()
        val ref = "$nextRef"
        nextRef++
        messages[ref] = result
        conn.sendSerialized(RawPhoenixChannelMessage(joinRef, ref, topic, event, payload))
        return result.await()
    }

    suspend fun disconnect() = conn.close()

    suspend fun run(): Unit = coroutineScope {
        val heartbeatJob = async {
            while (isActive) {
                delay(10.seconds)
                sendAsync(topic = "phoenix", event = "heartbeat")
            }
        }
        val receiveJob = async {
            conn.consumeEachIncoming { message ->
                if (message.ref != null) {
                    messages.remove(message.ref)?.apply {
                        complete(message)
                        return@consumeEachIncoming
                    }
                }
                if (message.joinRef != null) {
                    channels[message.joinRef]?.apply {
                        handle(message.event, message.payload)
                        return@consumeEachIncoming
                    }
                }
                println("Unhandled message")
                println(message)
            }
        }
        val onCancelJob = async {
            conn.closeReason.await()
            val ex = CancellationException("connection closed")
            heartbeatJob.cancel(ex)
            receiveJob.cancel(ex)
        }
        try {
            awaitAll(heartbeatJob, receiveJob, onCancelJob)
        } catch (_: CancellationException) {}
    }
}

suspend fun HttpClient.phoenixSocket(request: HttpRequestBuilder.() -> Unit) =
    PhoenixSocket(
        this.webSocketSession {
                request()
                url { appendPathSegments("websocket") }
                parameter("vsn", "2.0.0")
            }
            .wrap()
    )
