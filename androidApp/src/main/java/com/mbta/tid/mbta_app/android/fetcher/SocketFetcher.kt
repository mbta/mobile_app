package com.mbta.tid.mbta_app.android.fetcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.android.util.decodeJson
import org.phoenixframework.Payload
import org.phoenixframework.Socket

@Composable
internal inline fun <reified T> subscribeToSocket(
    socket: Socket,
    topic: String,
    payload: Payload?,
    newDataEvent: String
): T? {
    var data by remember { mutableStateOf<T?>(null) }

    DisposableEffect(topic, payload, newDataEvent) {
        val channel =
            if (payload != null) {
                socket.channel(topic, payload).also { channel ->
                    channel.on(newDataEvent) { data = it.decodeJson() }
                    channel.join().receive("ok") { data = it.decodeJson() }
                }
            } else {
                null
            }
        onDispose { channel?.leave() }
    }

    return data
}
