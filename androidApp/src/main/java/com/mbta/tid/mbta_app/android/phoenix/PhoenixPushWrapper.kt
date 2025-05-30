package com.mbta.tid.mbta_app.android.phoenix

import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixPush
import com.mbta.tid.mbta_app.network.PhoenixPushStatus
import org.phoenixframework.Push

@JvmInline
value class PhoenixPushWrapper(private val push: Push) : PhoenixPush {
    override fun receive(
        status: PhoenixPushStatus,
        callback: (PhoenixMessage) -> Unit,
    ): PhoenixPush = push.receive(status.value, callback.unwrapped()).wrapped()
}

fun Push.wrapped() = PhoenixPushWrapper(this)
