package com.mbta.tid.mbta_app.network

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop

public enum class PhoenixPushStatus(public val value: String) {
    Ok("ok"),
    Error("error"),
    Timeout("timeout"),
}

public interface PhoenixMessage {
    public val subject: String
    public val body: Map<String, Any?>
    public val jsonBody: String?
}

public interface PhoenixPush {
    public fun receive(status: PhoenixPushStatus, callback: ((PhoenixMessage) -> Unit)): PhoenixPush
}

public interface PhoenixChannel {
    public fun onEvent(event: String, callback: ((PhoenixMessage) -> Unit))

    public fun onFailure(callback: ((message: PhoenixMessage) -> Unit))

    public fun onDetach(callback: ((PhoenixMessage) -> Unit))

    public fun attach(): PhoenixPush

    public fun detach(): PhoenixPush
}

public interface PhoenixSocket {
    public fun onAttach(callback: () -> Unit): String

    public fun onDetach(callback: () -> Unit): String

    public fun attach()

    public fun getChannel(topic: String, params: Map<String, Any>): PhoenixChannel

    public fun detach()
}

public class MockPhoenixSocket
@DefaultArgumentInterop.Enabled
constructor(
    private val onAttachCallback: () -> Unit = {},
    private val onDetatchCallback: () -> Unit = {},
) : PhoenixSocket {
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

internal class MockPhoenixChannel : PhoenixChannel {
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

internal class MockPush : PhoenixPush {
    override fun receive(
        status: PhoenixPushStatus,
        callback: (PhoenixMessage) -> Unit,
    ): PhoenixPush {
        return MockPush()
    }
}
