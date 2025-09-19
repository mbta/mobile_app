package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.network.receiveAll

internal class ChannelOwner(private val socket: PhoenixSocket) {
    internal var channel: PhoenixChannel? = null

    fun connect(
        spec: ChannelSpec,
        handleMessage: (PhoenixMessage) -> Unit,
        handleJoin: (PhoenixMessage) -> Unit = handleMessage,
        handleError: (message: String) -> Unit,
    ) {
        disconnect()
        channel = socket.getChannel(spec.topic, spec.params)

        channel?.onEvent(spec.updateEvent, handleMessage)
        channel?.onFailure { handleError(SocketError.FAILURE) }

        channel?.onDetach { message -> println("leaving channel ${message.subject}") }
        channel
            ?.attach()
            ?.receiveAll(
                onOk = { message ->
                    println("joined channel ${message.subject}")
                    handleJoin(message)
                },
                onError = { handleError(SocketError.RECEIVED_ERROR) },
                onTimeout = { handleError(SocketError.TIMEOUT) },
            )
    }

    fun disconnect() {
        channel?.detach()
        channel = null
    }
}
