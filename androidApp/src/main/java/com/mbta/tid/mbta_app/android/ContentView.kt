package com.mbta.tid.mbta_app.android

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.Backend
import com.mbta.tid.mbta_app.android.fetcher.fetchGlobalData
import com.mbta.tid.mbta_app.android.fetcher.subscribeToAlerts
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitPage
import com.mbta.tid.mbta_app.android.util.decodeMessage
import com.mbta.tid.mbta_app.phoenix.SocketUtils
import io.github.dellisd.spatialk.geojson.Position
import org.phoenixframework.Socket

@Composable
fun ContentView() {
    val backend = remember { Backend() }
    val socket = remember { Socket(SocketUtils.url, decode = ::decodeMessage) }
    val alertData = subscribeToAlerts(socket = socket)
    val globalData = fetchGlobalData(backend = backend)
    val mapCenter = Position(longitude = -71.062424, latitude = 42.356395)

    DisposableEffect(null) {
        socket.connect()
        socket.onMessage { message -> Log.i("Socket", message.toString()) }
        socket.onError { throwable, response -> Log.e("Socket", response.toString(), throwable) }
        onDispose { socket.disconnect() }
    }

    NearbyTransitPage(
        Modifier.fillMaxSize(),
        backend = backend,
        socket = socket,
        alertData = alertData,
        globalData = globalData,
        targetLocation = mapCenter,
    )
}
