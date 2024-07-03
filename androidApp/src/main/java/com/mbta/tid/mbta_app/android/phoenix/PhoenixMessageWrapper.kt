package com.mbta.tid.mbta_app.android.phoenix

import com.mbta.tid.mbta_app.network.PhoenixMessage
import org.phoenixframework.Message

@JvmInline
value class PhoenixMessageWrapper(private val message: Message) : PhoenixMessage {
    override val subject
        get() = message.topic

    override val body: Map<String, Any?>
        get() = message.payload

    override val jsonBody: String
        get() = message.payloadJson
}

fun Message.wrapped() = PhoenixMessageWrapper(this)

fun ((PhoenixMessage) -> Unit).unwrapped() = { message: Message -> this.invoke(message.wrapped()) }
