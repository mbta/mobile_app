package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.network.receiveAll
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ChannelOwner(
    private val socket: PhoenixSocket,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    internal var channel: PhoenixChannel? = null
    private val connectLock = Mutex()

    fun connect(
        spec: ChannelSpec,
        handleMessage: (PhoenixMessage) -> Unit,
        handleJoin: (PhoenixMessage) -> Unit = handleMessage,
        handleError: (message: String) -> Unit,
    ) {
        disconnect()
        CoroutineScope(dispatcher).launch {
            connectLock.withLock {
                val channel = socket.getChannel(spec.topic, spec.params)

                channel.onEvent(spec.updateEvent, handleMessage)
                channel.onFailure { handleError(SocketError.FAILURE) }

                channel.onDetach { message -> println("leaving channel ${message.subject}") }
                channel
                    .attach()
                    .receiveAll(
                        onOk = { message ->
                            println("joined channel ${message.subject}")
                            handleJoin(message)
                        },
                        onError = { handleError(SocketError.RECEIVED_ERROR) },
                        onTimeout = { handleError(SocketError.TIMEOUT) },
                    )
                this@ChannelOwner.channel = channel
            }
        }
    }

    fun disconnect() {
        channel?.detach()
        channel = null
    }
}
