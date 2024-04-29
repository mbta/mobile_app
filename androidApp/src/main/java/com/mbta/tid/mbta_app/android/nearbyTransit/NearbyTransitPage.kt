package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.Backend
import com.mbta.tid.mbta_app.android.fetcher.GlobalData
import com.mbta.tid.mbta_app.android.fetcher.getSchedules
import com.mbta.tid.mbta_app.android.fetcher.subscribeToPredictions
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.withRealtimeInfo
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds
import org.phoenixframework.Socket

@Composable
fun NearbyTransitPage(
    modifier: Modifier = Modifier,
    backend: Backend,
    socket: Socket,
    alertData: AlertsStreamDataResponse?,
    globalData: GlobalData,
    targetLocation: Position,
    setLastLocation: (Position) -> Unit,
) {
    var nearby by remember { mutableStateOf<NearbyResponse?>(null) }
    val nearbyByRouteAndStop =
        remember(globalData, nearby) {
            val globalResponse = globalData.response
            val nearbyData = nearby
            if (globalResponse != null && nearbyData != null) {
                NearbyStaticData(globalResponse, nearbyData)
            } else {
                null
            }
        }
    val stopIds = remember(nearby) { nearby?.stopIds }
    val now = timer(updateInterval = 5.seconds)
    val schedules = getSchedules(backend, stopIds, now)
    val predictions = subscribeToPredictions(socket = socket, stopIds = stopIds)
    val nearbyWithRealtimeInfo =
        remember(nearbyByRouteAndStop, targetLocation, schedules, predictions, alertData, now) {
            nearbyByRouteAndStop?.withRealtimeInfo(
                sortByDistanceFrom = targetLocation,
                schedules,
                predictions,
                alertData,
                now,
                pinnedRoutes = setOf()
            )
        }

    LaunchedEffect(targetLocation) {
        nearby =
            backend.getNearby(
                latitude = targetLocation.latitude,
                longitude = targetLocation.longitude
            )
        setLastLocation(targetLocation)
    }

    if (nearbyWithRealtimeInfo != null) {
        LazyColumn(modifier) { items(nearbyWithRealtimeInfo) { NearbyRouteView(it, now) } }
    } else {
        Text(text = "Loading...", modifier)
    }
}
