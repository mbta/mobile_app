package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext

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

    val tripFilter = TripDetailsFilter(trip.id, vehicle.id, stopSequence)

    val koinApplication = testKoinApplication(objects)

    @Test
    fun testOpensDownstreamStop() {
        val openedSheetRoutes = mutableListOf<SheetRoutes>()
        val loggedEvents = mutableListOf<Pair<String, Map<String, String>>>()
        val analytics =
            MockAnalytics(
                onLogEvent = { event, properties -> loggedEvents.add(event to properties) }
            )

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val viewModel =
                    stopDetailsManagedVM(
                        filters =
                            StopDetailsPageFilters(
                                stop.id,
                                StopDetailsFilter(route.id, routePattern.directionId),
                                tripFilter,
                            ),
                        globalResponse,
                        alertData,
                        updateStopFilter = { _, _ -> },
                        updateTripFilter = { _, _ -> },
                        setMapSelectedVehicle = {},
                        now,
                    )

                TripDetailsView(
                    tripFilter,
                    stopId = stop.id,
                    allAlerts = alertData,
                    alertSummaries = emptyMap(),
                    stopDetailsVM = viewModel,
                    onOpenAlertDetails = {},
                    openSheetRoute = openedSheetRoutes::add,
                    openModal = {},
                    now,
                    analytics,
                )
            }
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

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val viewModel =
                    stopDetailsManagedVM(
                        filters =
                            StopDetailsPageFilters(
                                stop.id,
                                StopDetailsFilter(route.id, routePattern.directionId),
                                tripFilter,
                            ),
                        globalResponse,
                        AlertsStreamDataResponse(objects),
                        updateStopFilter = { _, _ -> },
                        updateTripFilter = { _, _ -> },
                        setMapSelectedVehicle = {},
                        now,
                    )

                TripDetailsView(
                    tripFilter,
                    stopId = stop.id,
                    allAlerts = AlertsStreamDataResponse(objects),
                    alertSummaries = emptyMap(),
                    stopDetailsVM = viewModel,
                    onOpenAlertDetails = {},
                    openSheetRoute = openedSheetRoutes::add,
                    openModal = {},
                    now,
                    analytics,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(downstreamStop.name).assertIsDisplayed()
        composeTestRule.onNodeWithText("Stop Closed").assertIsDisplayed()
    }
}
