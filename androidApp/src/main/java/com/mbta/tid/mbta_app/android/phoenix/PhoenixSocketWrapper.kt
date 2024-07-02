package com.mbta.tid.mbta_app.android.phoenix

import android.util.Log
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixSocket
import org.phoenixframework.Socket

@JvmInline
value class PhoenixSocketWrapper(private val socket: Socket) : PhoenixSocket {
    override fun onAttach(callback: () -> Unit): String = socket.onOpen(callback)

    override fun onDetach(callback: () -> Unit): String = socket.onClose(callback)

    override fun attach() = socket.connect()

    override fun getChannel(topic: String, params: Map<String, Any>): PhoenixChannel =
        socket.channel(topic, params).wrapped()

    override fun detach() = socket.disconnect()

    fun attachLogging() {
        socket.onMessage { message -> Log.i("Socket", message.toString()) }
        socket.onError { throwable, response -> Log.e("Socket", response.toString(), throwable) }
    }
}

fun Socket.wrapped() = PhoenixSocketWrapper(this)
