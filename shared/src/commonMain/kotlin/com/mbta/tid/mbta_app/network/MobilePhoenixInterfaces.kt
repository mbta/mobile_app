package com.mbta.tid.mbta_app.network

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop

enum class PhoenixPushStatus(val value: String) {
    Ok("ok"),
    Error("error"),
    Timeout("timeout")
}

interface PhoenixMessage {
    val subject: String
    val body: Map<String, Any?>
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

class MockPhoenixSocket
@DefaultArgumentInterop.Enabled
constructor(val onAttachCallback: () -> Unit = {}, val onDetatchCallback: () -> Unit = {}) :
    PhoenixSocket {
    override fun onAttach(callback: () -> Unit): String {
        return "attached"
    }

    override fun onDetach(callback: () -> Unit): String {
        return "detached"
    }

    override fun attach() {
        onAttachCallback()
    }

    override fun getChannel(topic: String, params: Map<String, Any>): PhoenixChannel {
        return MockPhoenixChannel()
    }

    override fun detach() {
        onDetatchCallback()
    }
}

class MockPhoenixChannel : PhoenixChannel {
    override fun onEvent(event: String, callback: (PhoenixMessage) -> Unit) {}

    override fun onFailure(callback: (message: PhoenixMessage) -> Unit) {}

    override fun onDetach(callback: (PhoenixMessage) -> Unit) {}

    override fun attach(): PhoenixPush {
        return MockPush()
    }

    override fun detach(): PhoenixPush {
        return MockPush()
    }
}

class MockPush : PhoenixPush {
    override fun receive(
        status: PhoenixPushStatus,
        callback: (PhoenixMessage) -> Unit
    ): PhoenixPush {
        return MockPush()
    }
}
