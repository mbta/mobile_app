package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
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
    schedulesRepository: ISchedulesRepository = koinInject(),
    pinnedRoutesRepository: IPinnedRoutesRepository = koinInject(),
    togglePinnedRouteUsecase: TogglePinnedRouteUsecase = koinInject()
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

    var pinnedRoutes: Set<String>? by remember { mutableStateOf(null) }
    LaunchedEffect(null) { pinnedRoutes = pinnedRoutesRepository.getPinnedRoutes() }

    val nearbyWithRealtimeInfo =
        remember(nearby, targetLocation, schedules, predictions, alertData, now, pinnedRoutes) {
            nearby?.withRealtimeInfo(
                sortByDistanceFrom = targetLocation,
                schedules,
                predictions,
                alertData,
                now,
                pinnedRoutes.orEmpty()
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

    val coroutineScope = rememberCoroutineScope()

    val togglePinnedRoute: (String) -> Unit = { routeId ->
        coroutineScope.launch {
            togglePinnedRouteUsecase.execute(routeId)
            pinnedRoutes = pinnedRoutesRepository.getPinnedRoutes()
        }
    }

    if (nearbyWithRealtimeInfo != null) {
        LazyColumn(modifier) {
            item {
                Text(
                    text = "Nearby transit",
                    modifier = Modifier.padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(nearbyWithRealtimeInfo) {
                when (it) {
                    is StopsAssociated.WithRoute ->
                        NearbyRouteView(
                            it,
                            pinnedRoutes.orEmpty().contains(it.id),
                            togglePinnedRoute,
                            now
                        )
                    is StopsAssociated.WithLine ->
                        NearbyLineView(
                            it,
                            pinnedRoutes.orEmpty().contains(it.id),
                            togglePinnedRoute,
                            now
                        )
                }
            }
        }
    } else {
        Text(text = "Loading...", modifier)
    }
}
