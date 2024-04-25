package com.mbta.tid.mbta_app.android.util

import com.mbta.tid.mbta_app.json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.phoenixframework.Message

fun decodeMessage(message: String): Message {
    val el: JsonElement = json.decodeFromString(message)
    val joinRef = el.jsonArray[0].jsonPrimitive.contentOrNull
    val ref = el.jsonArray[1].jsonPrimitive.contentOrNull ?: ""
    val topic = el.jsonArray[2].jsonPrimitive.content
    val event = el.jsonArray[3].jsonPrimitive.content
    val payload = el.jsonArray[4].jsonObject
    return Message(joinRef, ref, topic, event, payload)
}

inline fun <reified T> Message.decodeJson(): T = json.decodeFromJsonElement(payload as JsonObject)
