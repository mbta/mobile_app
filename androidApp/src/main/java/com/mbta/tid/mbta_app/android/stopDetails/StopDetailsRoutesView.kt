package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.StopDetailsFilter
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Composable
fun StopDetailsRoutesView(
    departures: StopDetailsDepartures,
    global: GlobalResponse?,
    now: Instant,
    filterState: MutableState<StopDetailsFilter?>,
    pinRoute: (String) -> Unit,
    pinnedRoutes: Set<String>
) {
    val filter by filterState

    if (filter != null) {
        StopDetailsFilteredRouteView(
            departures = departures,
            global = global,
            now = now,
            filterState = filterState
        )
    } else {
        LazyColumn(Modifier.padding(top = 16.dp).background(colorResource(R.color.fill1))) {
            items(departures.routes, key = { it.routeIdentifier }) { patternsByStop ->
                StopDetailsRouteView(
                    patternsByStop,
                    now,
                    pinned = pinnedRoutes.contains(patternsByStop.routeIdentifier),
                    onPin = pinRoute
                )
            }
        }
    }
}

@Preview
@Composable
private fun StopDetailsRoutesViewPreview() {
    val objects = ObjectCollectionBuilder()

    val route1 =
        objects.route {
            color = "00843D"
            longName = "Green Line B"
            textColor = "FFFFFF"
            type = RouteType.LIGHT_RAIL
        }
    val route2 =
        objects.route {
            color = "FFC72C"
            shortName = "57"
            textColor = "000000"
            type = RouteType.BUS
        }
    val stop = objects.stop()
    val trip1 = objects.trip()
    val prediction1 =
        objects.prediction {
            trip = trip1
            departureTime = Clock.System.now() + 5.minutes
        }
    val trip2 = objects.trip()
    val schedule2 =
        objects.schedule {
            trip = trip2
            departureTime = Clock.System.now() + 10.minutes
        }
    val trip3 = objects.trip()
    val prediction2 =
        objects.prediction {
            trip = trip3
            departureTime = Clock.System.now() + 8.minutes
        }

    MyApplicationTheme {
        StopDetailsRoutesView(
            departures =
                StopDetailsDepartures(
                    listOf(
                        PatternsByStop(
                            route1,
                            stop,
                            listOf(
                                RealtimePatterns.ByHeadsign(
                                    route1,
                                    "A",
                                    null,
                                    trip1.routePatternId,
                                    emptyList(),
                                    listOf(UpcomingTrip(trip1, prediction1))
                                )
                            )
                        ),
                        PatternsByStop(
                            route2,
                            stop,
                            listOf(
                                RealtimePatterns.ByHeadsign(
                                    route2,
                                    "B",
                                    null,
                                    trip3.routePatternId,
                                    emptyList(),
                                    listOf(UpcomingTrip(trip3, prediction2))
                                ),
                                RealtimePatterns.ByHeadsign(
                                    route2,
                                    "C",
                                    null,
                                    trip2.routePatternId,
                                    emptyList(),
                                    listOf(UpcomingTrip(trip2, schedule2))
                                )
                            )
                        )
                    )
                ),
            global = null,
            now = Clock.System.now(),
            filterState = remember { mutableStateOf(null) },
            pinRoute = {},
            pinnedRoutes = emptySet()
        )
    }
}
