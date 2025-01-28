package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test

class StopDetailsFilteredDeparturesViewTest {
    val builder = ObjectCollectionBuilder()
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    val route =
        builder.route {
            id = "route_1"
            type = RouteType.BUS
            color = "FF0000"
            directionNames = listOf("North", "South")
            directionDestinations = listOf("Downtown", "Uptown")
            longName = "Sample Route Long Name"
            shortName = "Sample Route"
            textColor = "000000"
            lineId = "line_1"
            routePatternIds = mutableListOf("pattern_1", "pattern_2")
        }
    val routePatternOne =
        builder.routePattern(route) {
            id = "pattern_1"
            directionId = 0
            name = "Sample Route Pattern"
            routeId = "route_1"
            representativeTripId = "trip_1"
        }
    val routePatternTwo =
        builder.routePattern(route) {
            id = "pattern_2"
            directionId = 1
            name = "Sample Route Pattern Two"
            routeId = "route_1"
            representativeTripId = "trip_1"
        }
    val stop =
        builder.stop {
            id = "stop_1"
            name = "Sample Stop"
            locationType = LocationType.STOP
            latitude = 0.0
            longitude = 0.0
        }
    val line =
        builder.line {
            id = "line_1"
            color = "FF0000"
            textColor = "FFFFFF"
        }
    val trip =
        builder.trip {
            id = "trip_1"
            routeId = "route_1"
            directionId = 0
            headsign = "Sample Headsign"
            routePatternId = "pattern_1"
        }
    val prediction =
        builder.prediction {
            id = "prediction_1"
            revenue = true
            stopId = "stop_1"
            tripId = "trip_1"
            routeId = "route_1"
            stopSequence = 1
            directionId = 0
            arrivalTime = now.plus(1.minutes)
            departureTime = now.plus(1.5.minutes)
        }

    private val globalResponse =
        GlobalResponse(
            builder,
            mutableMapOf(
                stop.id to listOf(routePatternOne.id, routePatternTwo.id),
            )
        )

    private val errorBannerViewModel =
        ErrorBannerViewModel(false, MockErrorBannerStateRepository(), MockSettingsRepository())

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testStopDetailsRouteViewDisplaysCorrectly() {
        val departures =
            checkNotNull(
                StopDetailsDepartures.fromData(
                    stop,
                    globalResponse,
                    null,
                    PredictionsStreamDataResponse(builder),
                    AlertsStreamDataResponse(emptyMap()),
                    emptySet(),
                    now,
                    useTripHeadsigns = false,
                )
            )
        val viewModel = StopDetailsViewModel.mocked()
        viewModel.setDepartures(departures)

        composeTestRule.setContent {
            val filterState = remember {
                mutableStateOf(StopDetailsFilter(routeId = route.id, directionId = 0))
            }

            StopDetailsFilteredDeparturesView(
                stopId = stop.id,
                stopFilter = filterState.value,
                tripFilter = null,
                patternsByStop = departures.routes.first { it.routeIdentifier == route.id },
                tileData =
                    departures.stopDetailsFormattedTrips(
                        filterState.value.routeId,
                        filterState.value.directionId,
                        now
                    ),
                elevatorAlerts = emptyList(),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = {},
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                setMapSelectedVehicle = {},
                openAlertDetails = {}
            )
        }

        composeTestRule.onNodeWithText("at ${stop.name}").assertExists()
        composeTestRule.onNodeWithText("Sample Headsign").assertExists()
        composeTestRule.onNodeWithText("1 min").assertExists()
    }

    @Test
    fun testTappingTripSetsFilter() {
        var tripFilter: TripDetailsFilter? = null

        val departures =
            checkNotNull(
                StopDetailsDepartures.fromData(
                    stop,
                    globalResponse,
                    null,
                    PredictionsStreamDataResponse(builder),
                    AlertsStreamDataResponse(emptyMap()),
                    emptySet(),
                    now,
                    useTripHeadsigns = false,
                )
            )
        val viewModel = StopDetailsViewModel.mocked()
        viewModel.setDepartures(departures)

        composeTestRule.setContent {
            val filterState = remember {
                mutableStateOf(StopDetailsFilter(routeId = route.id, directionId = 0))
            }

            StopDetailsFilteredDeparturesView(
                stopId = stop.id,
                stopFilter = filterState.value,
                tripFilter = null,
                patternsByStop = departures.routes.first { it.routeIdentifier == route.id },
                tileData =
                    departures.stopDetailsFormattedTrips(
                        filterState.value.routeId,
                        filterState.value.directionId,
                        now
                    ),
                elevatorAlerts = emptyList(),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = { tripFilter = it },
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                setMapSelectedVehicle = {},
                openAlertDetails = {}
            )
        }

        composeTestRule.onNodeWithText("at ${stop.name}").assertExists()
        composeTestRule.onNodeWithText("Sample Headsign").assertExists().performClick()
        composeTestRule.waitUntil { tripFilter?.tripId == trip.id }

        assertEquals(tripFilter?.tripId, trip.id)
    }
}
