package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripData
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripHeaderSpec
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.MockTripDetailsViewModel
import com.mbta.tid.mbta_app.viewModel.TripDetailsViewModel
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TripDetailsViewTest {
    @get:Rule val composeTestRule = createComposeRule()

    val now = EasternTimeInstant.now()
    val objects = ObjectCollectionBuilder()
    val route = objects.route()
    val routePattern = objects.routePattern(route)
    val stop = objects.stop()
    val trip = objects.trip(routePattern)
    val vehicle = objects.vehicle { currentStatus = Vehicle.CurrentStatus.InTransitTo }
    val stopSequence = 10

    val downstreamStopSequence = 20
    lateinit var downstreamStop: Stop
    val downstreamStopParent =
        objects.stop { downstreamStop = childStop { name = "North Station" } }
    val schedule =
        objects.schedule {
            this.trip = this@TripDetailsViewTest.trip
            stopId = downstreamStop.id
            stopSequence = downstreamStopSequence
            departureTime = now + 5.minutes
        }
    val prediction = objects.prediction(schedule) { departureTime = now + 5.minutes }

    val globalResponse = GlobalResponse(objects)
    val alertData = AlertsStreamDataResponse(objects)

    val tripFilter = TripDetailsPageFilter(trip.id, vehicle.id, route.id, 0, stop.id, stopSequence)
    val tripData =
        TripData(
            tripFilter,
            trip,
            TripSchedulesResponse.Schedules(listOf(schedule)),
            PredictionsStreamDataResponse(objects),
            true,
            vehicle,
        )

    @Before
    fun setUp() {
        loadKoinMocks(objects)
    }

    @Test
    fun testOpensDownstreamStop() {
        val openedSheetRoutes = mutableListOf<SheetRoutes>()
        val loggedEvents = mutableListOf<Pair<String, Map<String, String>>>()
        val analytics =
            MockAnalytics(
                onLogEvent = { event, properties -> loggedEvents.add(event to properties) }
            )
        val tripDetailsStopList = runBlocking {
            TripDetailsStopList.fromPieces(
                trip,
                tripData.tripSchedules,
                tripData.tripPredictions,
                vehicle,
                alertData,
                globalResponse,
            )
        }

        val tripVMState = TripDetailsViewModel.State(tripData, tripDetailsStopList)
        val viewModel = MockTripDetailsViewModel(tripVMState)

        composeTestRule.setContent {
            TripDetailsView(
                tripFilter,
                allAlerts = alertData,
                alertSummaries = emptyMap(),
                onOpenAlertDetails = {},
                openSheetRoute = openedSheetRoutes::add,
                openModal = {},
                now,
                tripDetailsVM = viewModel,
                analytics = analytics,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(downstreamStop.name).performClick()
        assertEquals<List<SheetRoutes>>(
            listOf(SheetRoutes.StopDetails(downstreamStopParent.id, null, null)),
            openedSheetRoutes,
        )
        assertEquals(
            listOf(
                Pair(
                    "tapped_downstream_stop",
                    mapOf(
                        "route_id" to route.id,
                        "stop_id" to downstreamStopParent.id,
                        "trip_id" to trip.id,
                        "connecting_route_id" to "",
                    ),
                )
            ),
            loggedEvents,
        )
    }

    @Test
    fun testDownstreamAlertShown() {
        val openedSheetRoutes = mutableListOf<SheetRoutes>()
        val loggedEvents = mutableListOf<Pair<String, Map<String, String>>>()
        val analytics =
            MockAnalytics(
                onLogEvent = { event, properties -> loggedEvents.add(event to properties) }
            )

        objects.alert {
            cause = Alert.Cause.Parade
            effect = Alert.Effect.StopClosure
            activePeriod(now.minus(5.minutes), now.plus(30.minutes))
            informedEntity(
                activities =
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride,
                    ),
                directionId = 0,
                route = route.id,
                stop = downstreamStop.id,
            )
        }

        val alerts = AlertsStreamDataResponse(objects)
        val tripDetailsStopList = runBlocking {
            TripDetailsStopList.fromPieces(
                trip,
                tripData.tripSchedules,
                tripData.tripPredictions,
                vehicle,
                alerts,
                globalResponse,
            )
        }

        val tripVMState = TripDetailsViewModel.State(tripData, tripDetailsStopList)
        val viewModel = MockTripDetailsViewModel(tripVMState)

        composeTestRule.setContent {
            TripDetailsView(
                tripFilter,
                allAlerts = alerts,
                alertSummaries = emptyMap(),
                onOpenAlertDetails = {},
                openSheetRoute = openedSheetRoutes::add,
                openModal = {},
                now,
                tripDetailsVM = viewModel,
                analytics = analytics,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(downstreamStop.name).assertIsDisplayed()
        composeTestRule.onNodeWithText("Stop Closed").assertIsDisplayed()
    }

    @Test
    fun testFollowButtonWhenTrackThisTrip() {
        loadKoinMocks(objects) {
            settings = MockSettingsRepository(mapOf(Settings.TrackThisTrip to true))
        }

        var onFollowCalled = false

        composeTestRule.setContent {
            TripDetails(
                trip = trip,
                headerSpec = TripHeaderSpec.VehicleOnTrip(vehicle, stop, null, false),
                onHeaderTap = null,
                onOpenAlertDetails = {},
                onFollowTrip = { onFollowCalled = true },
                onTapStop = {},
                route = route,
                tripFilter = tripFilter,
                stopList = TripDetailsStopList(trip, emptyList()),
                now = now,
                alertSummaries = emptyMap(),
                globalResponse = GlobalResponse(objects),
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Follow"))
        composeTestRule.onNodeWithText("Follow").performClick()
        assertTrue(onFollowCalled)
    }

    @Test
    fun testNoFollowButtonByDefault() {
        loadKoinMocks { settings = MockSettingsRepository(mapOf(Settings.TrackThisTrip to false)) }

        composeTestRule.setContent {
            TripDetails(
                trip = trip,
                headerSpec = TripHeaderSpec.VehicleOnTrip(vehicle, stop, null, false),
                onHeaderTap = null,
                onOpenAlertDetails = {},
                onFollowTrip = {},
                onTapStop = {},
                route = route,
                tripFilter = tripFilter,
                stopList = TripDetailsStopList(trip, emptyList()),
                now = now,
                alertSummaries = emptyMap(),
                globalResponse = GlobalResponse(objects),
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Follow").assertDoesNotExist()
    }
}
