package com.mbta.tid.mbta_app.viewModel

import app.cash.turbine.test
import com.mbta.tid.mbta_app.dependencyInjection.MockRepositories
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripData
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.TestData
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest

class TripDetailsPageViewModelTest : KoinTest {
    private fun setUpKoin(
        objects: ObjectCollectionBuilder,
        repositoriesBlock: MockRepositories.() -> Unit = {},
    ) {
        startKoin {
            modules(
                repositoriesModule(
                    MockRepositories().apply {
                        useObjects(objects)
                        repositoriesBlock()
                    }
                )
            )
        }
    }

    @AfterTest
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun `uses trip headsign in direction`() = runTest {
        val objects = TestData.clone()
        val route = objects.getRoute("Red")
        val routePattern = objects.getRoutePattern("Red-1-0")
        val trip = objects.trip(routePattern)
        val targetStop = objects.getStop("place-knncl")

        setUpKoin(objects)

        val tripFilter =
            TripDetailsPageFilter(
                tripId = trip.id,
                vehicleId = null,
                routeId = route.id,
                directionId = trip.directionId,
                stopId = targetStop.id,
                stopSequence = null,
            )
        val tripState =
            TripDetailsViewModel.State(
                tripData =
                    TripData(
                        tripFilter = tripFilter,
                        trip = trip,
                        tripSchedules = null,
                        tripPredictions = null,
                        vehicle = null,
                    )
            )
        val tripDetailsVMModels = MutableStateFlow(tripState)
        val tripDetailsVM =
            mock<ITripDetailsViewModel>(MockMode.autofill) {
                every { models } returns tripDetailsVMModels
            }
        val vm = TripDetailsPageViewModel(tripDetailsVM)
        vm.setFilter(tripFilter)
        testViewModelFlow(vm).test {
            assertEquals(
                TripDetailsPageViewModel.State(
                    Direction("South", "Ashmont", trip.directionId),
                    alertSummaries = emptyMap(),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `falls back to direction destination before trip loads`() = runTest {
        val objects = TestData.clone()
        val route = objects.getRoute("Red")
        val routePattern = objects.getRoutePattern("Red-1-0")
        val trip = objects.trip(routePattern)
        val targetStop = objects.getStop("place-knncl")

        setUpKoin(objects)

        val tripFilter =
            TripDetailsPageFilter(
                tripId = trip.id,
                vehicleId = null,
                routeId = route.id,
                directionId = trip.directionId,
                stopId = targetStop.id,
                stopSequence = null,
            )
        val tripState = TripDetailsViewModel.State(tripData = null)
        val tripDetailsVMModels = MutableStateFlow(tripState)
        val tripDetailsVM =
            mock<ITripDetailsViewModel>(MockMode.autofill) {
                every { models } returns tripDetailsVMModels
            }
        val vm = TripDetailsPageViewModel(tripDetailsVM)
        vm.setFilter(tripFilter)
        testViewModelFlow(vm).test {
            assertEquals(
                TripDetailsPageViewModel.State(
                    Direction("South", "Ashmont/Braintree", trip.directionId),
                    alertSummaries = emptyMap(),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `calculates summary for alerts`() = runTest {
        val objects = TestData.clone()
        val route = objects.getRoute("Red")
        val routePattern = objects.getRoutePattern("Red-1-0")
        val trip = objects.trip(routePattern)
        val targetStop = objects.getStop("place-knncl")
        val alertStop = objects.getStop("place-fldcr")

        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                activePeriod(EasternTimeInstant.now() - 1.hours, null)
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride,
                    ),
                    route = route.id,
                    stop = alertStop.id,
                )
            }

        setUpKoin(objects)

        val tripFilter =
            TripDetailsPageFilter(
                tripId = trip.id,
                vehicleId = null,
                routeId = route.id,
                directionId = trip.directionId,
                stopId = targetStop.id,
                stopSequence = null,
            )
        val tripState = TripDetailsViewModel.State(tripData = null)
        val tripDetailsVMModels = MutableStateFlow(tripState)
        val tripDetailsVM =
            mock<ITripDetailsViewModel>(MockMode.autofill) {
                every { models } returns tripDetailsVMModels
            }
        val vm = TripDetailsPageViewModel(tripDetailsVM)
        vm.setAlerts(AlertsStreamDataResponse(objects))
        vm.setFilter(tripFilter)
        testViewModelFlow(vm).test {
            assertEquals(
                TripDetailsPageViewModel.State(
                    Direction("South", "Ashmont/Braintree", trip.directionId),
                    alertSummaries =
                        mapOf(
                            alert.id to
                                AlertSummary(
                                    effect = Alert.Effect.Suspension,
                                    location = AlertSummary.Location.SingleStop(alertStop.name),
                                    timeframe = null,
                                )
                        ),
                ),
                awaitItemSatisfying { it.alertSummaries.isNotEmpty() },
            )
        }
    }
}
