package com.mbta.tid.mbta_app.android.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.phoenixframework.Message
import org.phoenixframework.Payload

fun decodeMessage(rawMessage: String): Message {
    val message = Json.parseToJsonElement(rawMessage).jsonArray
    val joinRef = message[0].jsonPrimitive.contentOrNull

    val ref = message[1].jsonPrimitive.contentOrNull
    val topic = message[2].jsonPrimitive.contentOrNull
    val event = message[3].jsonPrimitive.contentOrNull

    val rawPayload: Payload
    val payloadJson: String
    if (message[4].jsonObject.keys == setOf("status", "response")) {
        rawPayload =
            mapOf("status" to message[4].jsonObject["status"]?.jsonPrimitive?.contentOrNull)
        payloadJson = message[4].jsonObject["response"]?.toString() ?: "{}"
    } else {
        rawPayload = emptyMap()
        payloadJson = message[4].toString()
    }

    return Message(
        joinRef = joinRef,
        ref = ref ?: "",
        topic = topic ?: "",
        event = event ?: "",
        rawPayload = rawPayload,
        payloadJson = payloadJson
    )
}
