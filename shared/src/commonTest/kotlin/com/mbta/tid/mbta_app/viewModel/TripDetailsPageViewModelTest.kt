package com.mbta.tid.mbta_app.viewModel

import app.cash.turbine.test
import com.mbta.tid.mbta_app.dependencyInjection.MockRepositories
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RoutePattern
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
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
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
                    trip = trip,
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
                    trip = null,
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
                    route = route.id.idText,
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
                                    timeframe = AlertSummary.Timeframe.UntilFurtherNotice,
                                )
                        ),
                    trip = null,
                ),
                awaitItemSatisfying { it.alertSummaries.isNotEmpty() },
            )
        }
    }

    @Test
    fun `preserves all patterns in alert summary`() = runTest {
        val objects = TestData.clone()
        val route = objects.getRoute("Red")
        val fullRoutePattern = objects.getRoutePattern("Red-3-0")
        val truncatedRoutePattern =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip {
                    headsign = "North Quincy"
                    stopIds =
                        listOf(
                            "70061",
                            "70063",
                            "70065",
                            "70067",
                            "70069",
                            "70071",
                            "70073",
                            "70075",
                            "70077",
                            "70079",
                            "70081",
                            "70083",
                            "70095",
                            "70097",
                        )
                }
            }
        val trip = objects.trip(truncatedRoutePattern)
        val targetStop = objects.getStop("place-pktrm")

        val alert =
            objects.alert {
                effect = Alert.Effect.Shuttle
                activePeriod(
                    EasternTimeInstant.now() - 1.hours,
                    EasternTimeInstant(
                        (EasternTimeInstant.now().serviceDate + DatePeriod(days = 1)).atTime(3, 0)
                    ),
                )
                val boardExitRide =
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride,
                    )
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board, Alert.InformedEntity.Activity.Ride),
                    directionId = 0,
                    route = route.id.idText,
                    routeType = route.type,
                    stop = "70097",
                )
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Exit, Alert.InformedEntity.Activity.Ride),
                    directionId = 1,
                    route = route.id.idText,
                    routeType = route.type,
                    stop = "70098",
                )
                informedEntity(
                    boardExitRide,
                    directionId = 0,
                    route = route.id.idText,
                    routeType = route.type,
                    stop = "70099",
                )
                informedEntity(
                    boardExitRide,
                    directionId = 1,
                    route = route.id.idText,
                    routeType = route.type,
                    stop = "70100",
                )
                informedEntity(
                    boardExitRide,
                    directionId = 0,
                    route = route.id.idText,
                    routeType = route.type,
                    stop = "70101",
                )
                informedEntity(
                    boardExitRide,
                    directionId = 1,
                    route = route.id.idText,
                    routeType = route.type,
                    stop = "70102",
                )
                informedEntity(
                    boardExitRide,
                    directionId = 0,
                    route = route.id.idText,
                    routeType = route.type,
                    stop = "70103",
                )
                informedEntity(
                    boardExitRide,
                    directionId = 1,
                    route = route.id.idText,
                    routeType = route.type,
                    stop = "70104",
                )
                informedEntity(
                    boardExitRide,
                    route = route.id.idText,
                    routeType = route.type,
                    stop = "70105",
                )
                informedEntity(
                    boardExitRide,
                    route = route.id.idText,
                    routeType = route.type,
                    stop = "place-brntn",
                )
                informedEntity(
                    boardExitRide,
                    route = route.id.idText,
                    routeType = route.type,
                    stop = "place-nqncy",
                )
                informedEntity(
                    boardExitRide,
                    route = route.id.idText,
                    routeType = route.type,
                    stop = "place-qamnl",
                )
                informedEntity(
                    boardExitRide,
                    route = route.id.idText,
                    routeType = route.type,
                    stop = "place-qnctr",
                )
                informedEntity(
                    boardExitRide,
                    route = route.id.idText,
                    routeType = route.type,
                    stop = "place-wlsta",
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
        vm.setAlerts(AlertsStreamDataResponse(objects))
        vm.setFilter(tripFilter)
        testViewModelFlow(vm).test {
            assertEquals(
                TripDetailsPageViewModel.State(
                    Direction("South", "North Quincy", trip.directionId),
                    alertSummaries =
                        mapOf(
                            alert.id to
                                AlertSummary(
                                    effect = Alert.Effect.Shuttle,
                                    location =
                                        AlertSummary.Location.SuccessiveStops(
                                            "North Quincy",
                                            "Braintree",
                                        ),
                                    timeframe = AlertSummary.Timeframe.EndOfService,
                                )
                        ),
                    trip = trip,
                ),
                awaitItemSatisfying { it.alertSummaries.isNotEmpty() },
            )
        }
    }
}
