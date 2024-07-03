package com.mbta.tid.mbta_app.android.phoenix

import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixPush
import org.phoenixframework.Channel

@JvmInline
value class PhoenixChannelWrapper(private val channel: Channel) : PhoenixChannel {
    override fun onEvent(event: String, callback: (PhoenixMessage) -> Unit) {
        channel.on(event, callback.unwrapped())
    }

    override fun onFailure(callback: (message: PhoenixMessage) -> Unit) {
        channel.onError(callback.unwrapped())
    }

    override fun onDetach(callback: (PhoenixMessage) -> Unit) {
        channel.onClose(callback.unwrapped())
    }

    override fun attach(): PhoenixPush = channel.join().wrapped()

    override fun detach(): PhoenixPush = channel.leave().wrapped()
}

fun Channel.wrapped() = PhoenixChannelWrapper(this)
