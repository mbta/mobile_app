package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.android.fetcher.GlobalData
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.Coordinate
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.StopsAssociated
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.model.withRealtimeInfo
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds
import org.koin.compose.koinInject

@Composable
fun NearbyTransitPage(
    modifier: Modifier = Modifier,
    alertData: AlertsStreamDataResponse?,
    globalData: GlobalData,
    targetLocation: Position,
    setLastLocation: (Position) -> Unit,
    nearbyRepository: INearbyRepository = koinInject(),
    predictionsRepository: IPredictionsRepository = koinInject(),
    schedulesRepository: ISchedulesRepository = koinInject()
) {
    var nearby: NearbyStaticData? by remember { mutableStateOf(null) }
    val stopIds = remember(nearby) { nearby?.stopIds()?.toList() }
    val now = timer(updateInterval = 5.seconds)
    var schedules: ScheduleResponse? by remember { mutableStateOf(null) }
    LaunchedEffect(stopIds, now) {
        if (stopIds != null) {
            schedules = schedulesRepository.getSchedule(stopIds, now)
        }
    }
    var predictions: PredictionsStreamDataResponse? by remember { mutableStateOf(null) }
    DisposableEffect(stopIds) {
        if (stopIds != null) {
            predictionsRepository.connect(stopIds) { predictions = it.data }
        }
        onDispose { predictionsRepository.disconnect() }
    }
    val nearbyWithRealtimeInfo =
        remember(nearby, targetLocation, schedules, predictions, alertData, now) {
            nearby?.withRealtimeInfo(
                sortByDistanceFrom = targetLocation,
                schedules,
                predictions,
                alertData,
                now,
                pinnedRoutes = setOf()
            )
        }

    LaunchedEffect(targetLocation) {
        if (globalData.response != null) {
            nearby =
                nearbyRepository.getNearby(
                    global = globalData.response,
                    location =
                        Coordinate(
                            latitude = targetLocation.latitude,
                            longitude = targetLocation.longitude
                        )
                )
            setLastLocation(targetLocation)
        }
    }

    if (nearbyWithRealtimeInfo != null) {
        LazyColumn(modifier) {
            items(nearbyWithRealtimeInfo) {
                when (it) {
                    is StopsAssociated.WithRoute -> NearbyRouteView(it, now)
                    is StopsAssociated.WithLine -> {}
                }
            }
        }
    } else {
        Text(text = "Loading...", modifier)
    }
}
