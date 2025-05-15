package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext

class StopDetailsFilteredDeparturesViewTest {
    val builder = ObjectCollectionBuilder()
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    val route =
        builder.route {
            id = "route_1"
            type = RouteType.LIGHT_RAIL
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
    val downstreamStop =
        builder.stop {
            id = "stop_2"
            name = "Sample Stop 2"
            locationType = LocationType.STOP
            latitude = 0.0
            longitude = 0.0
        }
    val stop =
        builder.stop {
            id = "stop_1"
            name = "Sample Stop"
            locationType = LocationType.STOP
            latitude = 0.0
            longitude = 0.0
        }
    val inaccessibleStop =
        builder.stop {
            id = "stop_3"
            name = "Sample Stop 3"
            locationType = LocationType.STOP
            latitude = 0.0
            longitude = 0.0
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE
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
            stopIds = listOf(stop.id, downstreamStop.id, inaccessibleStop.id)
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
                inaccessibleStop.id to listOf(routePatternOne.id, routePatternTwo.id),
            ),
        )

    private val settings = mutableMapOf<Settings, Boolean>()
    private val settingsRepository =
        object : ISettingsRepository {
            override suspend fun getSettings() = settings

            override suspend fun setSettings(settings: Map<Settings, Boolean>) {}
        }

    private val koinApplication = testKoinApplication { settings = settingsRepository }

    @get:Rule val composeTestRule = createComposeRule()

    @Before fun resetSettings() = settings.clear()

    @Test
    fun testStopDetailsRouteViewDisplaysCorrectly(): Unit = runBlocking {
        val filterState = StopDetailsFilter(routeId = route.id, directionId = 0)
        val viewModel = StopDetailsViewModel.mocked()
        val routeCardData =
            checkNotNull(
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id),
                    globalResponse,
                    null,
                    null,
                    PredictionsStreamDataResponse(builder),
                    AlertsStreamDataResponse(emptyMap()),
                    now,
                    emptySet(),
                    context = RouteCardData.Context.StopDetailsFiltered,
                )
            )
        val routeStopData = routeCardData.single().stopData.single()
        val leaf = routeStopData.data.first { it.directionId == 0 }
        viewModel.setRouteCardData(routeCardData)

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                StopDetailsFilteredDeparturesView(
                    stopId = stop.id,
                    stopFilter = filterState,
                    tripFilter = null,
                    leaf = leaf,
                    selectedDirection = routeStopData.directions.first(),
                    allAlerts = null,
                    global = globalResponse,
                    now = now,
                    viewModel = viewModel,
                    updateTripFilter = {},
                    tileScrollState = rememberScrollState(),
                    pinnedRoutes = emptySet(),
                    openModal = {},
                    openSheetRoute = {},
                )
            }
        }

        composeTestRule.onNodeWithText("1 min").assertExists()
    }

    @Test
    fun testTappingTripSetsFilter() = runBlocking {
        var tripFilter: TripDetailsFilter? = null

        val filterState = StopDetailsFilter(routeId = route.id, directionId = 0)
        val viewModel = StopDetailsViewModel.mocked()

        val routeCardData =
            checkNotNull(
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id),
                    globalResponse,
                    null,
                    null,
                    PredictionsStreamDataResponse(builder),
                    AlertsStreamDataResponse(emptyMap()),
                    now,
                    emptySet(),
                    context = RouteCardData.Context.StopDetailsFiltered,
                )
            )
        val routeStopData = routeCardData.single().stopData.single()
        val leaf = routeStopData.data.first { it.directionId == 0 }
        viewModel.setRouteCardData(routeCardData)

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                StopDetailsFilteredDeparturesView(
                    stopId = stop.id,
                    stopFilter = filterState,
                    tripFilter = null,
                    leaf = leaf,
                    selectedDirection = routeStopData.directions.first(),
                    allAlerts = null,
                    global = globalResponse,
                    now = now,
                    viewModel = viewModel,
                    updateTripFilter = { tripFilter = it },
                    tileScrollState = rememberScrollState(),
                    pinnedRoutes = emptySet(),
                    openModal = {},
                    openSheetRoute = {},
                )
            }
        }

        composeTestRule.onNodeWithText("1 min").assertExists().performClick()
        composeTestRule.waitUntil { tripFilter?.tripId == trip.id }

        assertEquals(tripFilter?.tripId, trip.id)
    }

    @Test
    fun testShowsCancelledTripCard() {
        val objects = ObjectCollectionBuilder()
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val route =
            objects.route {
                id = "route_1"
                type = RouteType.BUS
                color = "DA291C"
                routePatternIds = mutableListOf("pattern_1")
            }
        val routePattern =
            objects.routePattern(route) {
                id = "pattern_1"
                directionId = 0
                representativeTripId = "trip_1"
            }

        val stop = objects.stop { id = "stop_1" }
        val trip =
            objects.trip {
                id = "trip_1"
                routeId = "route_1"
                directionId = 0
                routePatternId = "pattern_1"
            }

        val schedule =
            objects.schedule {
                tripId = "trip_1"
                stopId = "stop_1"
                departureTime = now.plus(10.minutes)
            }
        val prediction =
            objects.prediction {
                id = "prediction_1"
                stopId = "stop_1"
                tripId = "trip_1"
                routeId = "route_1"
                directionId = 0
                departureTime = now.plus(10.minutes)
                scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
            }

        val globalResponse =
            GlobalResponse(objects, mutableMapOf(stop.id to listOf(routePattern.id)))

        val viewModel = StopDetailsViewModel.mocked()

        val lineOrRoute = RouteCardData.LineOrRoute.Route(route)
        val leaf =
            RouteCardData.Leaf(
                lineOrRoute,
                stop,
                trip.directionId,
                listOf(routePattern),
                setOf(stop.id),
                listOf(UpcomingTrip(trip, schedule, prediction)),
                alertsHere = emptyList(),
                allDataLoaded = true,
                hasSchedulesToday = true,
                alertsDownstream = emptyList(),
                RouteCardData.Context.StopDetailsFiltered,
            )
        val routeStopData = RouteCardData.RouteStopData(route, stop, listOf(leaf), globalResponse)
        val routeCardData = RouteCardData(lineOrRoute, listOf(routeStopData), now)

        viewModel.setRouteCardData(listOf(routeCardData))

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                StopDetailsFilteredDeparturesView(
                    stopId = stop.id,
                    stopFilter =
                        StopDetailsFilter(routeId = route.id, directionId = trip.directionId),
                    tripFilter = TripDetailsFilter(trip.id, null, null, false),
                    leaf = leaf,
                    selectedDirection = routeStopData.directions.first(),
                    allAlerts = null,
                    global = globalResponse,
                    now = now,
                    viewModel = viewModel,
                    updateTripFilter = {},
                    tileScrollState = rememberScrollState(),
                    pinnedRoutes = emptySet(),
                    openModal = {},
                    openSheetRoute = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Trip cancelled").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("This trip has been cancelled. We’re sorry for the inconvenience.")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("route_slash_icon", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun testShowsNoTripCard() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route = objects.route { id = "Green-B" }
        val line = objects.line { id = "Green" }

        val lineOrRoute = RouteCardData.LineOrRoute.Line(line, setOf(route))
        val leaf =
            RouteCardData.Leaf(
                lineOrRoute,
                stop,
                0,
                emptyList(),
                setOf(stop.id),
                emptyList(),
                emptyList(),
                true,
                true,
                emptyList(),
                RouteCardData.Context.StopDetailsFiltered,
            )

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                StopDetailsFilteredDeparturesView(
                    stopId = stop.id,
                    stopFilter = StopDetailsFilter(route.id, 0),
                    tripFilter = null,
                    leaf = leaf,
                    selectedDirection = Direction(null, null, 0),
                    allAlerts = null,
                    global = globalResponse,
                    now = now,
                    viewModel = StopDetailsViewModel.mocked(),
                    updateTripFilter = {},
                    tileScrollState = rememberScrollState(),
                    pinnedRoutes = emptySet(),
                    openModal = {},
                    openSheetRoute = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Service ended").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testShowsSuspension(): Unit = runBlocking {
        val now = Clock.System.now()
        val alert =
            builder.alert {
                activePeriod(now - 5.seconds, now + 5.seconds)
                effect = Alert.Effect.Suspension
                header = "Fuchsia Line suspended from Here to There"
                informedEntity(
                    activities =
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Exit,
                            Alert.InformedEntity.Activity.Ride,
                        ),
                    directionId = 0,
                    route = route.id,
                    stop = stop.id,
                )
            }
        val alertResponse = AlertsStreamDataResponse(mapOf(alert.id to alert))

        val filterState = StopDetailsFilter(routeId = route.id, directionId = 0)
        val viewModel = StopDetailsViewModel.mocked()

        val routeCardData =
            checkNotNull(
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id),
                    globalResponse,
                    null,
                    null,
                    PredictionsStreamDataResponse(builder),
                    alertResponse,
                    now,
                    emptySet(),
                    context = RouteCardData.Context.StopDetailsFiltered,
                )
            )
        val routeStopData = routeCardData.single().stopData.single()
        val leaf = routeStopData.data.first { it.directionId == 0 }
        viewModel.setRouteCardData(routeCardData)

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                StopDetailsFilteredDeparturesView(
                    stopId = stop.id,
                    stopFilter = filterState,
                    tripFilter = null,
                    leaf = leaf,
                    selectedDirection = routeStopData.directions.first(),
                    allAlerts = null,
                    global = globalResponse,
                    now = now,
                    viewModel = viewModel,
                    updateTripFilter = {},
                    tileScrollState = rememberScrollState(),
                    pinnedRoutes = emptySet(),
                    openModal = {},
                    openSheetRoute = {},
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Service suspended at ${stop.name}", true)
        )
        composeTestRule.onNodeWithText("View details").assertHasClickAction()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testShowsPredictionsAndAlertOnBranchingTrunk(): Unit = runBlocking {
        val now = Clock.System.now()

        val objects = TestData.clone()
        val stop = objects.getStop("place-kencl")
        val line = objects.getLine("line-Green")
        val routeB = objects.getRoute("Green-B")
        val routeC = objects.getRoute("Green-C")
        val routeD = objects.getRoute("Green-D")

        val alert =
            objects.alert {
                activePeriod(now - 5.seconds, now + 20.minutes)
                effect = Alert.Effect.Shuttle
                header = "Green line shuttle on B and C branches"
                informedEntity(
                    activities =
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Exit,
                            Alert.InformedEntity.Activity.Ride,
                        ),
                    directionId = 0,
                    route = routeB.id,
                    stop = "71151",
                )
                informedEntity(
                    activities =
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Exit,
                            Alert.InformedEntity.Activity.Ride,
                        ),
                    directionId = 0,
                    route = routeC.id,
                    stop = "70151",
                )
            }
        val alertResponse = AlertsStreamDataResponse(mapOf(alert.id to alert))

        objects.upcomingTrip(
            objects.prediction {
                departureTime = now.plus(5.minutes)
                routeId = routeD.id
                stopId = stop.id
                trip =
                    objects.trip {
                        routeId = routeD.id
                        routePatternId = "Green-D-855-0"
                    }
            }
        )

        val filterState = StopDetailsFilter(routeId = line.id, directionId = 0)
        val viewModel = StopDetailsViewModel.mocked()
        val global = GlobalResponse(objects)
        val routeCardData =
            checkNotNull(
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id) + stop.childStopIds,
                    global,
                    null,
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    alertResponse,
                    now,
                    emptySet(),
                    context = RouteCardData.Context.StopDetailsFiltered,
                )
            )
        val routeStopData = routeCardData.single().stopData.single()
        val leaf = routeStopData.data.first { it.directionId == 0 }
        viewModel.setRouteCardData(routeCardData)

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                StopDetailsFilteredDeparturesView(
                    stopId = stop.id,
                    stopFilter = filterState,
                    tripFilter = null,
                    leaf = leaf,
                    selectedDirection = routeStopData.directions.first(),
                    allAlerts = null,
                    global = global,
                    now = now,
                    viewModel = viewModel,
                    updateTripFilter = {},
                    tileScrollState = rememberScrollState(),
                    pinnedRoutes = emptySet(),
                    openModal = {},
                    openSheetRoute = {},
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.waitUntilExactlyOneExists(hasText("Shuttle buses at ${stop.name}", true))
        composeTestRule.onNodeWithText("5 min").assertExists()
    }

    @Test
    fun testShowsDownstreamAlert(): Unit = runBlocking {
        val alert =
            builder.alert {
                activePeriod(now - 5.seconds, now + 5.seconds)
                effect = Alert.Effect.Suspension
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
        val alertResponse = AlertsStreamDataResponse(mapOf(alert.id to alert))

        val filterState = StopDetailsFilter(routeId = route.id, directionId = 0)
        val viewModel = StopDetailsViewModel.mocked()

        val routeCardData =
            checkNotNull(
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id),
                    globalResponse,
                    null,
                    null,
                    PredictionsStreamDataResponse(builder),
                    alertResponse,
                    now,
                    emptySet(),
                    context = RouteCardData.Context.StopDetailsFiltered,
                )
            )
        val routeStopData = routeCardData.single().stopData.single()
        val leaf = routeStopData.data.first { it.directionId == 0 }
        viewModel.setRouteCardData(routeCardData)

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                StopDetailsFilteredDeparturesView(
                    stopId = stop.id,
                    stopFilter = filterState,
                    tripFilter = null,
                    leaf = leaf,
                    selectedDirection = routeStopData.directions.first(),
                    allAlerts = alertResponse,
                    global = globalResponse,
                    now = now,
                    viewModel = viewModel,
                    updateTripFilter = {},
                    tileScrollState = rememberScrollState(),
                    pinnedRoutes = emptySet(),
                    openModal = {},
                    openSheetRoute = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Service suspended", true).assertIsDisplayed()
    }

    @Test
    fun testShowsElevatorAlertOnlyOnce(): Unit = runBlocking {
        settings[Settings.StationAccessibility] = true
        val alert =
            builder.alert {
                effect = Alert.Effect.ElevatorClosure
                header = "Elevator Alert Header"
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.UsingWheelchair),
                    stop = stop.id,
                )
                activePeriod(now - 30.minutes, null)
            }

        val filterState = StopDetailsFilter(routeId = route.id, directionId = 0)
        val viewModel = StopDetailsViewModel.mocked()

        val routeCardData =
            checkNotNull(
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id),
                    globalResponse,
                    null,
                    null,
                    PredictionsStreamDataResponse(builder),
                    AlertsStreamDataResponse(builder),
                    now,
                    emptySet(),
                    context = RouteCardData.Context.StopDetailsFiltered,
                )
            )
        val routeStopData = routeCardData.single().stopData.single()
        val leaf = routeStopData.data.first { it.directionId == 0 }
        viewModel.setRouteCardData(routeCardData)
        viewModel.setAlertSummaries(mapOf(alert.id to null))

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                StopDetailsFilteredDeparturesView(
                    stopId = stop.id,
                    stopFilter = filterState,
                    tripFilter = null,
                    leaf = leaf,
                    selectedDirection = routeStopData.directions.first(),
                    allAlerts = null,
                    global = globalResponse,
                    now = now,
                    viewModel = viewModel,
                    updateTripFilter = {},
                    tileScrollState = rememberScrollState(),
                    pinnedRoutes = emptySet(),
                    openModal = {},
                    openSheetRoute = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Elevator Closure").assertDoesNotExist()
        composeTestRule.onNodeWithText("Elevator Alert Header").assertIsDisplayed()
    }

    @Test
    fun testShowsSubwayDelayAlert(): Unit = runBlocking {
        val now = Clock.System.now()
        val alert =
            builder.alert {
                activePeriod(now - 5.seconds, now + 5.seconds)
                effect = Alert.Effect.Delay
                header = "Delays alert header"
                cause = Alert.Cause.HeavyRidership
                severity = 10
                informedEntity(
                    activities =
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Exit,
                            Alert.InformedEntity.Activity.Ride,
                        ),
                    directionId = 0,
                    route = route.id,
                    routeType = RouteType.LIGHT_RAIL,
                    stop = stop.id,
                )
            }
        val alertResponse = AlertsStreamDataResponse(mapOf(alert.id to alert))

        val filterState = StopDetailsFilter(routeId = route.id, directionId = 0)
        val viewModel = StopDetailsViewModel.mocked()

        val routeCardData =
            checkNotNull(
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id),
                    globalResponse,
                    null,
                    null,
                    PredictionsStreamDataResponse(builder),
                    alertResponse,
                    now,
                    emptySet(),
                    context = RouteCardData.Context.StopDetailsFiltered,
                )
            )
        val routeStopData = routeCardData.single().stopData.single()
        val leaf = routeStopData.data.first { it.directionId == 0 }
        viewModel.setRouteCardData(routeCardData)

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                StopDetailsFilteredDeparturesView(
                    stopId = stop.id,
                    stopFilter = filterState,
                    tripFilter = null,
                    leaf = leaf,
                    selectedDirection = routeStopData.directions.first(),
                    allAlerts = null,
                    global = globalResponse,
                    now = now,
                    viewModel = viewModel,
                    updateTripFilter = {},
                    tileScrollState = rememberScrollState(),
                    pinnedRoutes = emptySet(),
                    openModal = {},
                    openSheetRoute = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Delays due to heavy ridership").assertIsDisplayed()
    }

    @Test
    fun testShowsNotAccessibleAlert(): Unit = runBlocking {
        settings[Settings.StationAccessibility] = true
        val filterState = StopDetailsFilter(routeId = route.id, directionId = 0)
        val viewModel = StopDetailsViewModel.mocked()

        val routeCardData =
            checkNotNull(
                RouteCardData.routeCardsForStopList(
                    listOf(inaccessibleStop.id),
                    globalResponse,
                    null,
                    null,
                    PredictionsStreamDataResponse(builder),
                    AlertsStreamDataResponse(emptyMap()),
                    now,
                    emptySet(),
                    RouteCardData.Context.StopDetailsFiltered,
                )
            )
        val routeStopData = routeCardData.single().stopData.single()
        val leaf = routeStopData.data.first { it.directionId == 0 }
        viewModel.setRouteCardData(routeCardData)

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                StopDetailsFilteredDeparturesView(
                    stopId = inaccessibleStop.id,
                    stopFilter = filterState,
                    tripFilter = null,
                    leaf = leaf,
                    selectedDirection = routeStopData.directions.first(),
                    allAlerts = null,
                    global = globalResponse,
                    now = now,
                    viewModel = viewModel,
                    updateTripFilter = {},
                    tileScrollState = rememberScrollState(),
                    pinnedRoutes = emptySet(),
                    openModal = {},
                    openSheetRoute = {},
                )
            }
        }

        composeTestRule.onNodeWithText("This stop is not accessible").assertIsDisplayed()
    }
}
