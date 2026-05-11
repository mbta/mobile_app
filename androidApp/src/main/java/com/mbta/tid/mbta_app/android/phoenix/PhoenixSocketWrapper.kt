package com.mbta.tid.mbta_app.android.phoenix

import android.util.Log
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixSocket
import org.phoenixframework.Socket

@JvmInline
value class PhoenixSocketWrapper(private val socket: Socket) : PhoenixSocket {

    init {
        socket.timeout = 6_000
        socket.reconnectAfterMs = { tries ->
            if (tries > 9) 2_000
            else listOf(10L, 50L, 100L, 150L, 200L, 250L, 500L, 1_000L, 2_000L)[tries - 1]
        }

        socket.rejoinAfterMs = { tries ->
            if (tries > 2) 2_000 else listOf(1_000L, 2_000L)[tries - 1]
        }
    }

    override fun onAttach(callback: () -> Unit): String = socket.onOpen(callback)

    override fun onDetach(callback: () -> Unit): String = socket.onClose(callback)

    override fun onError(callback: (Throwable, String) -> Unit) {
        socket.onError { throwable, response ->
            callback(throwable, response?.message ?: "no message")
        }
    }

    override fun attach() = socket.connect()

    override fun getChannel(topic: String, params: Map<String, Any>): PhoenixChannel =
        socket.channel(topic, params).wrapped()

    override fun detach() = socket.disconnect()

    fun attachLogging() {
        socket.onError { throwable, response ->
            Log.e("Socket", response?.toString() ?: throwable.toString(), throwable)
        }
    }
}

fun Socket.wrapped() = PhoenixSocketWrapper(this)
