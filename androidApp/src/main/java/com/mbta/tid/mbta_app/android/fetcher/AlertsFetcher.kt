package com.mbta.tid.mbta_app.android.fetcher

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.phoenix.AlertsChannel
import org.phoenixframework.Socket

@Composable
fun subscribeToAlerts(socket: Socket): AlertsStreamDataResponse? =
    subscribeToSocket(
        socket,
        topic = AlertsChannel.topic,
        payload = emptyMap(),
        newDataEvent = AlertsChannel.newDataEvent
    )
