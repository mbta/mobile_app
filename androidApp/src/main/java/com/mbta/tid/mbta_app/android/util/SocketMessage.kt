package com.mbta.tid.mbta_app.android.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.phoenixframework.Message

fun decodeMessage(rawMessage: String): Message {
    val message = Json.parseToJsonElement(rawMessage).jsonArray
    val joinRef = message[0].jsonPrimitive.contentOrNull

    val ref = message[1].jsonPrimitive.contentOrNull
    val topic = message[2].jsonPrimitive.contentOrNull
    val event = message[3].jsonPrimitive.contentOrNull

    val payloadJson = message[4].toString()

    return Message(
        joinRef = joinRef,
        ref = ref ?: "",
        topic = topic ?: "",
        event = event ?: "",
        rawPayload = emptyMap(),
        payloadJson = payloadJson
    )
}
