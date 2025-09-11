@file:JsModule("phoenix")

package com.mbta.tid.mbta_app.phoenix

import kotlin.js.Json

internal external class Channel {
    val topic: String

    fun join(): Push

    fun onClose(callback: (dynamic) -> Unit)

    fun onError(callback: (Json) -> Unit)

    fun on(event: String, callback: (Json) -> Unit)

    fun leave(): Push
}

internal external class Push {
    val channel: Channel
    val event: String

    fun receive(status: String, callback: (Json) -> Unit): Push
}

internal external class Socket(endPoint: String) {
    fun disconnect()

    fun connect()

    fun onOpen(callback: () -> Unit)

    fun onClose(callback: () -> Unit)

    fun channel(topic: String, chanParams: Any): Channel
}
