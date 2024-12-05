package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.state.getNearby
import com.mbta.tid.mbta_app.android.state.getSchedule
import com.mbta.tid.mbta_app.android.state.subscribeToPredictions
import com.mbta.tid.mbta_app.android.util.managePinnedRoutes
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.Coordinate
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopsAssociated
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.withRealtimeInfo
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds

@Composable
fun NearbyTransitView(
    modifier: Modifier = Modifier,
    alertData: AlertsStreamDataResponse?,
    globalResponse: GlobalResponse?,
    targetLocation: Position?,
    setLastLocation: (Position) -> Unit,
    onOpenStopDetails: (String, StopDetailsFilter?) -> Unit,
) {
    var nearby: NearbyStaticData? =
        getNearby(
            globalResponse,
            targetLocation?.let { Coordinate(latitude = it.latitude, longitude = it.longitude) },
            setLastLocation
        )
    val now = timer(updateInterval = 5.seconds)
    val stopIds = remember(nearby) { nearby?.stopIds()?.toList() }
    val schedules = getSchedule(stopIds)
    val predictions = subscribeToPredictions(stopIds)

    val (pinnedRoutes, togglePinnedRoute) = managePinnedRoutes()

    val nearbyWithRealtimeInfo =
        remember(
            nearby,
            globalResponse,
            targetLocation,
            schedules,
            predictions,
            alertData,
            now,
            pinnedRoutes
        ) {
            if (targetLocation != null) {
                nearby?.withRealtimeInfo(
                    globalData = globalResponse,
                    sortByDistanceFrom = targetLocation,
                    schedules,
                    predictions,
                    alertData,
                    now,
                    pinnedRoutes.orEmpty(),
                    useTripHeadsigns = false,
                )
            } else {
                null
            }
        }

    Column(modifier) {
        Text(
            text = "Nearby transit",
            modifier = Modifier.padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
            style = MaterialTheme.typography.titleLarge
        )

        if (nearbyWithRealtimeInfo == null) {
            Text(text = "Loading...", modifier)
        } else if (nearbyWithRealtimeInfo.isEmpty()) {
            Column(Modifier.padding(8.dp).weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    stringResource(R.string.no_stops_nearby_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    stringResource(R.string.no_stops_nearby),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(modifier) {
                items(nearbyWithRealtimeInfo) {
                    when (it) {
                        is StopsAssociated.WithRoute ->
                            NearbyRouteView(
                                it,
                                pinnedRoutes.orEmpty().contains(it.id),
                                togglePinnedRoute,
                                now,
                                onOpenStopDetails
                            )
                        is StopsAssociated.WithLine ->
                            NearbyLineView(
                                it,
                                pinnedRoutes.orEmpty().contains(it.id),
                                togglePinnedRoute,
                                now,
                                onOpenStopDetails
                            )
                    }
                }
            }
        }
    }
}
