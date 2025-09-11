package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixPush
import com.mbta.tid.mbta_app.network.PhoenixPushStatus
import com.mbta.tid.mbta_app.network.PhoenixSocket
import kotlin.js.Json
import kotlin.js.json

public class JsPhoenixSocket(socketUrl: String) : PhoenixSocket {
    private val inner = Socket(socketUrl)

    override fun onAttach(callback: () -> Unit): String {
        inner.onOpen(callback)
        return ""
    }

    override fun onDetach(callback: () -> Unit): String {
        inner.onClose(callback)
        return ""
    }

    override fun attach() {
        inner.connect()
    }

    override fun getChannel(topic: String, params: Map<String, Any>): PhoenixChannel {
        val resultParams = json()
        params.forEach { (k, v) -> resultParams[k] = v }
        return JsPhoenixChannel(inner.channel(topic, resultParams))
    }

    override fun detach() {
        inner.disconnect()
    }
}

public class JsPhoenixChannel internal constructor(private val inner: Channel) : PhoenixChannel {
    override fun onEvent(event: String, callback: (PhoenixMessage) -> Unit) {
        return inner.on(event) { callback(JsPhoenixMessage.fromRaw(it, inner.topic)) }
    }

    override fun onFailure(callback: (PhoenixMessage) -> Unit) {
        inner.onError { callback(JsPhoenixMessage.fromRaw(it, inner.topic)) }
    }

    override fun onDetach(callback: (PhoenixMessage) -> Unit) {
        inner.onClose { callback(JsPhoenixMessage.fromRaw(it, inner.topic)) }
    }

    override fun attach(): PhoenixPush {
        return JsPhoenixPush(inner.join())
    }

    override fun detach(): PhoenixPush {
        return JsPhoenixPush(inner.leave())
    }
}

public class JsPhoenixPush internal constructor(private var inner: Push) : PhoenixPush {
    override fun receive(
        status: PhoenixPushStatus,
        callback: (PhoenixMessage) -> Unit,
    ): PhoenixPush {
        inner.receive(status.name) { callback(JsPhoenixMessage.fromRaw(it, inner.channel.topic)) }
        return this
    }
}

public class JsPhoenixMessage
internal constructor(
    override val subject: String,
    override val body: Map<String, Any?>,
    override val jsonBody: String?,
) : PhoenixMessage {
    internal companion object {
        fun fromRaw(data: dynamic, channelTopic: String): JsPhoenixMessage {
            if (jsTypeOf(data) == "object") {
                val data: Json = data
                val subject: String = data["topic"] as? String ?: channelTopic
                val bodyEntries: Array<Array<Any?>> = js("Object.entries(data)")
                val body = bodyEntries.associate { (key, value) -> Pair(key as String, value) }
                val jsonBody = JSON.stringify(data)
                return JsPhoenixMessage(subject, body, jsonBody)
            } else {
                return JsPhoenixMessage(channelTopic, emptyMap(), null)
            }
        }
    }
}
