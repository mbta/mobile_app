package com.mbta.tid.mbta_app.android.fetcher

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.phoenix.PredictionsForStopsChannel
import org.phoenixframework.Socket

@Composable
fun subscribeToPredictions(socket: Socket, stopIds: List<String>?): PredictionsStreamDataResponse? =
    subscribeToSocket(
        socket,
        topic = PredictionsForStopsChannel.topic,
        payload = stopIds?.let { PredictionsForStopsChannel.joinPayload(it) },
        newDataEvent = PredictionsForStopsChannel.newDataEvent
    )
