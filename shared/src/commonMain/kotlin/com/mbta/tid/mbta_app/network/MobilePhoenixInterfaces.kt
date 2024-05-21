package com.mbta.tid.mbta_app.network

enum class PhoenixPushStatus(val value: String) {
    Ok("ok"),
    Error("error"),
    Timeout("timeout")
}

interface PhoenixMessage {
    val subject: String
    val body: Map<String, Any>
    val jsonBody: String?
}

interface PhoenixPush {
    fun receive(status: PhoenixPushStatus, callback: ((PhoenixMessage) -> Unit)): PhoenixPush
}

interface PhoenixChannel {
    fun onEvent(event: String, callback: ((PhoenixMessage) -> Unit))

    fun onFailure(callback: ((message: PhoenixMessage) -> Unit))

    fun onDetach(callback: ((PhoenixMessage) -> Unit))

    fun attach(): PhoenixPush

    fun detach(): PhoenixPush
}

interface PhoenixSocket {
    fun onAttach(callback: () -> Unit): String

    fun onDetach(callback: () -> Unit): String

    fun attach()

    fun getChannel(topic: String, params: Map<String, Any>): PhoenixChannel

    fun detach()
}
