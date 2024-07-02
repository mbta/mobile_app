package com.mbta.tid.mbta_app.android.util

import org.phoenixframework.Message

fun decodeMessage(rawMessage: String): Message {
    // https://github.com/dsrees/JavaPhoenixClient/blob/1.3.1/src/main/kotlin/org/phoenixframework/Defaults.kt#L68
    val parseValue: (String) -> String? = { value ->
        when (value) {
            "null" -> null
            else -> value.replace("\"", "")
        }
    }

    var message = rawMessage
    message = message.removeRange(0, 1) // remove '['

    val joinRef = message.takeWhile { it != ',' } // take "join ref", "null" or "\"5\""
    message = message.removeRange(0, joinRef.length) // remove join ref
    message = message.removeRange(0, 1) // remove ','

    val ref = message.takeWhile { it != ',' } // take ref, "null" or "\"5\""
    message = message.removeRange(0, ref.length) // remove ref
    message = message.removeRange(0, 1) // remove ','

    val topic = message.takeWhile { it != ',' } // take topic, "\"topic\""
    message = message.removeRange(0, topic.length)
    message = message.removeRange(0, 1) // remove ','

    val event = message.takeWhile { it != ',' } // take event, "\"phx_reply\""
    message = message.removeRange(0, event.length)
    message = message.removeRange(0, 1) // remove ','

    val payloadJson = message.removeRange(message.length - 1, message.length) // remove ']'

    return Message(
        joinRef = parseValue(joinRef),
        ref = parseValue(ref) ?: "",
        topic = parseValue(topic) ?: "",
        event = parseValue(event) ?: "",
        rawPayload = emptyMap(),
        payloadJson = payloadJson
    )
}
