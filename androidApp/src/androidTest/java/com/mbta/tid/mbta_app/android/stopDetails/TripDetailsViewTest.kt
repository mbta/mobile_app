package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.MainApplication
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.TripResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import com.mbta.tid.mbta_app.model.response.VehicleStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.ITripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ITripRepository
import com.mbta.tid.mbta_app.repositories.IVehicleRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockScheduleRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.MockTripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockTripRepository
import com.mbta.tid.mbta_app.repositories.MockVehicleRepository
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class TripDetailsViewTest {
    @get:Rule val composeTestRule = createComposeRule()

    val now = Clock.System.now()
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
    val predictionsResponse = PredictionsStreamDataResponse(objects)
    val tripSchedulesResponse = TripSchedulesResponse.Schedules(listOf(schedule))

    val tripFilter = TripDetailsFilter(trip.id, vehicle.id, stopSequence)

    val koinModule = module {
        single<IErrorBannerStateRepository> { MockErrorBannerStateRepository() }
        single<IGlobalRepository> { MockGlobalRepository(globalResponse) }
        single<IPredictionsRepository> { MockPredictionsRepository() }
        single<ISchedulesRepository> { MockScheduleRepository() }
        single<ISettingsRepository> { MockSettingsRepository() }
        single<ITripPredictionsRepository> {
            MockTripPredictionsRepository(response = predictionsResponse)
        }
        single<ITripRepository> {
            MockTripRepository(
                tripSchedulesResponse = tripSchedulesResponse,
                tripResponse = TripResponse(trip)
            )
        }
        single<IVehicleRepository> {
            MockVehicleRepository(outcome = ApiResult.Ok(VehicleStreamDataResponse(vehicle)))
        }
    }
    val koinApplication = koinApplication {
        modules(koinModule, MainApplication.koinViewModelModule)
    }

    @Test
    fun testSetsMapSelectedVehicle() {
        val mapSelectedVehicleValues = mutableListOf<Vehicle?>()

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val viewModel =
                    stopDetailsManagedVM(
                        filters =
                            StopDetailsPageFilters(
                                stop.id,
                                StopDetailsFilter(route.id, routePattern.directionId),
                                tripFilter
                            ),
                        globalResponse,
                        alertData,
                        pinnedRoutes = emptySet(),
                        updateStopFilter = { _, _ -> },
                        updateTripFilter = { _, _ -> },
                        now
                    )

                TripDetailsView(
                    tripFilter,
                    stopId = stop.id,
                    allAlerts = null,
                    stopDetailsVM = viewModel,
                    setMapSelectedVehicle = mapSelectedVehicleValues::add,
                    openSheetRoute = {},
                    openModal = {},
                    now,
                    analytics = MockAnalytics()
                )
            }
        }

        composeTestRule.waitForIdle()

        assertEquals(mapSelectedVehicleValues, listOf(null, vehicle))
    }

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
                                tripFilter
                            ),
                        globalResponse,
                        alertData,
                        pinnedRoutes = emptySet(),
                        updateStopFilter = { _, _ -> },
                        updateTripFilter = { _, _ -> },
                        now
                    )

                TripDetailsView(
                    tripFilter,
                    stopId = stop.id,
                    allAlerts = null,
                    stopDetailsVM = viewModel,
                    setMapSelectedVehicle = {},
                    openSheetRoute = openedSheetRoutes::add,
                    openModal = {},
                    now,
                    analytics
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(downstreamStop.name).performClick()
        assertEquals<List<SheetRoutes>>(
            listOf(SheetRoutes.StopDetails(downstreamStopParent.id, null, null)),
            openedSheetRoutes
        )
        assertEquals(
            listOf(
                Pair(
                    "tapped_downstream_stop",
                    mapOf(
                        "route_id" to route.id,
                        "stop_id" to downstreamStopParent.id,
                        "trip_id" to trip.id,
                        "connecting_route_id" to ""
                    )
                )
            ),
            loggedEvents
        )
    }
}
