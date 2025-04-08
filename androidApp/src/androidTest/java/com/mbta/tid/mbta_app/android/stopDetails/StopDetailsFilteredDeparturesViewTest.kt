package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.TileData
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test

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
                inaccessibleStop.id to listOf(routePatternOne.id, routePatternTwo.id)
            )
        )

    private val errorBannerViewModel =
        ErrorBannerViewModel(false, MockErrorBannerStateRepository(), MockSettingsRepository())

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testStopDetailsRouteViewDisplaysCorrectlyPreGroupByDirection(): Unit = runBlocking {
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
                data =
                    FilteredDeparturesData.PreGroupByDirection(
                        patternsByStop = departures.routes.first { it.routeIdentifier == route.id }
                    ),
                tileData =
                    departures
                        .stopDetailsFormattedTrips(
                            filterState.value.routeId,
                            filterState.value.directionId,
                            now
                        )
                        .mapNotNull { TileData.fromUpcoming(it.upcoming, route, now) },
                noPredictionsStatus = null,
                allAlerts = null,
                elevatorAlerts = emptyList(),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {}
            )
        }

        composeTestRule.onNodeWithText("at ${stop.name}").assertExists()
        composeTestRule.onNodeWithText("1 min").assertExists()
    }

    @Test
    fun testStopDetailsRouteViewDisplaysCorrectlyPostGroupByDirection(): Unit = runBlocking {
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
                    context = RouteCardData.Context.StopDetailsFiltered
                )
            )
        val routeStopData = routeCardData.single().stopData.single()
        val leaf = routeStopData.data.first { it.directionId == 0 }
        val leafFormat =
            leaf.format(now, route, globalResponse, RouteCardData.Context.StopDetailsFiltered)
        val viewModel =
            StopDetailsViewModel.mocked(
                settingsRepository =
                    MockSettingsRepository(mapOf(Settings.GroupByDirection to true))
            )
        viewModel.loadSettings()
        viewModel.setRouteCardData(routeCardData)

        composeTestRule.setContent {
            val filterState = remember {
                mutableStateOf(StopDetailsFilter(routeId = route.id, directionId = 0))
            }

            StopDetailsFilteredDeparturesView(
                stopId = stop.id,
                stopFilter = filterState.value,
                tripFilter = null,
                data =
                    FilteredDeparturesData.PostGroupByDirection(
                        routeCardData = routeCardData.single(),
                        routeStopData = routeStopData,
                        leaf = leaf
                    ),
                tileData = leafFormat.tileData(),
                noPredictionsStatus = leafFormat.noPredictionsStatus(),
                allAlerts = null,
                elevatorAlerts = emptyList(),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {}
            )
        }

        composeTestRule.onNodeWithText("at ${stop.name}").assertExists()
        composeTestRule.onNodeWithText("1 min").assertExists()
    }

    @Test
    fun testTappingTripSetsFilterPreGroupByDirection() = runBlocking {
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
                data =
                    FilteredDeparturesData.PreGroupByDirection(
                        patternsByStop = departures.routes.first { it.routeIdentifier == route.id }
                    ),
                tileData =
                    departures
                        .stopDetailsFormattedTrips(
                            filterState.value.routeId,
                            filterState.value.directionId,
                            now
                        )
                        .mapNotNull { TileData.fromUpcoming(it.upcoming, route, now) },
                noPredictionsStatus = null,
                allAlerts = null,
                elevatorAlerts = emptyList(),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = { tripFilter = it },
                tileScrollState = rememberScrollState(),
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {}
            )
        }

        composeTestRule.onNodeWithText("at ${stop.name}").assertExists()
        composeTestRule.onNodeWithText("1 min").assertExists().performClick()
        composeTestRule.waitUntil { tripFilter?.tripId == trip.id }

        assertEquals(tripFilter?.tripId, trip.id)
    }

    @Test
    fun testTappingTripSetsFilterPostGroupByDirection() = runBlocking {
        var tripFilter: TripDetailsFilter? = null

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
                    context = RouteCardData.Context.StopDetailsFiltered
                )
            )
        val routeStopData = routeCardData.single().stopData.single()
        val leaf = routeStopData.data.first { it.directionId == 0 }
        val leafFormat =
            leaf.format(now, route, globalResponse, RouteCardData.Context.StopDetailsFiltered)
        val viewModel =
            StopDetailsViewModel.mocked(
                settingsRepository =
                    MockSettingsRepository(mapOf(Settings.GroupByDirection to true))
            )
        viewModel.loadSettings()
        viewModel.setRouteCardData(routeCardData)

        composeTestRule.setContent {
            val filterState = remember {
                mutableStateOf(StopDetailsFilter(routeId = route.id, directionId = 0))
            }

            StopDetailsFilteredDeparturesView(
                stopId = stop.id,
                stopFilter = filterState.value,
                tripFilter = null,
                data =
                    FilteredDeparturesData.PostGroupByDirection(
                        routeCardData = routeCardData.single(),
                        routeStopData = routeStopData,
                        leaf = leaf
                    ),
                tileData = leafFormat.tileData(),
                noPredictionsStatus = leafFormat.noPredictionsStatus(),
                allAlerts = null,
                elevatorAlerts = emptyList(),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = { tripFilter = it },
                tileScrollState = rememberScrollState(),
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {}
            )
        }

        composeTestRule.onNodeWithText("at ${stop.name}").assertExists()
        composeTestRule.onNodeWithText("1 min").assertExists().performClick()
        composeTestRule.waitUntil { tripFilter?.tripId == trip.id }

        assertEquals(tripFilter?.tripId, trip.id)
    }

    @Test
    fun testShowsCancelledTripCardPreGroupByDirection() {
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
            GlobalResponse(
                objects,
                mutableMapOf(
                    stop.id to listOf(routePattern.id),
                )
            )

        val viewModel = StopDetailsViewModel.mocked()

        val departures =
            StopDetailsDepartures(
                listOf(
                    PatternsByStop(
                        route,
                        stop,
                        listOf(
                            RealtimePatterns.ByHeadsign(
                                route,
                                trip.headsign,
                                null,
                                listOf(routePattern),
                                listOf(UpcomingTrip(trip, schedule, prediction))
                            )
                        )
                    )
                )
            )
        viewModel.setDepartures(departures)

        composeTestRule.setContent {
            StopDetailsFilteredDeparturesView(
                stopId = stop.id,
                stopFilter = StopDetailsFilter(routeId = route.id, directionId = trip.directionId),
                tripFilter = TripDetailsFilter(trip.id, null, null, false),
                data =
                    FilteredDeparturesData.PreGroupByDirection(
                        patternsByStop = departures.routes.first { it.routeIdentifier == route.id }
                    ),
                tileData =
                    departures
                        .stopDetailsFormattedTrips(route.id, trip.directionId, now)
                        .mapNotNull { TileData.fromUpcoming(it.upcoming, route, now) },
                noPredictionsStatus = null,
                allAlerts = null,
                elevatorAlerts = emptyList(),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {}
            )
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
    fun testShowsCancelledTripCardPostGroupByDirection() {
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
            GlobalResponse(
                objects,
                mutableMapOf(
                    stop.id to listOf(routePattern.id),
                )
            )

        val viewModel =
            StopDetailsViewModel.mocked(
                settingsRepository =
                    MockSettingsRepository(mapOf(Settings.GroupByDirection to true))
            )
        viewModel.loadSettings()

        val leaf =
            RouteCardData.Leaf(
                trip.directionId,
                listOf(routePattern),
                setOf(stop.id),
                listOf(UpcomingTrip(trip, schedule, prediction)),
                alertsHere = emptyList(),
                allDataLoaded = true,
                hasSchedulesToday = true,
                alertsDownstream = emptyList()
            )
        val leafFormat =
            leaf.format(now, route, globalResponse, RouteCardData.Context.StopDetailsFiltered)
        val routeStopData = RouteCardData.RouteStopData(stop, route, listOf(leaf), globalResponse)
        val routeCardData =
            RouteCardData(
                RouteCardData.LineOrRoute.Route(route),
                listOf(routeStopData),
                RouteCardData.Context.StopDetailsFiltered,
                now
            )

        viewModel.setRouteCardData(listOf(routeCardData))

        composeTestRule.setContent {
            StopDetailsFilteredDeparturesView(
                stopId = stop.id,
                stopFilter = StopDetailsFilter(routeId = route.id, directionId = trip.directionId),
                tripFilter = TripDetailsFilter(trip.id, null, null, false),
                data =
                    FilteredDeparturesData.PostGroupByDirection(routeCardData, routeStopData, leaf),
                tileData = leafFormat.tileData(),
                noPredictionsStatus = leafFormat.noPredictionsStatus(),
                allAlerts = null,
                elevatorAlerts = emptyList(),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {}
            )
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
    fun testShowsNoTripCardPreGroupByDirection() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route = objects.route { id = "Green-B" }
        val line = objects.line { id = "Green" }

        composeTestRule.setContent {
            StopDetailsFilteredDeparturesView(
                stopId = stop.id,
                stopFilter = StopDetailsFilter(route.id, 0),
                tripFilter = null,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                tileData = listOf(),
                noPredictionsStatus = UpcomingFormat.NoTripsFormat.ServiceEndedToday,
                allAlerts = null,
                elevatorAlerts = listOf(),
                data =
                    FilteredDeparturesData.PreGroupByDirection(
                        patternsByStop =
                            PatternsByStop(
                                routes = listOf(route),
                                line = line,
                                stop = stop,
                                patterns = listOf(),
                                directions = listOf(),
                                elevatorAlerts = listOf()
                            )
                    ),
                global = globalResponse,
                now = now,
                viewModel = StopDetailsViewModel.mocked(),
                errorBannerViewModel = errorBannerViewModel,
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {}
            )
        }

        composeTestRule.onNodeWithText("Service ended").assertIsDisplayed()
    }

    @Test
    fun testShowsNoTripCardPostGroupByDirection() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route = objects.route { id = "Green-B" }
        val line = objects.line { id = "Green" }

        val leaf =
            RouteCardData.Leaf(
                0,
                emptyList(),
                setOf(stop.id),
                emptyList(),
                emptyList(),
                true,
                true,
                emptyList()
            )
        val routeStopData =
            RouteCardData.RouteStopData(
                stop,
                line,
                setOf(route),
                listOf(leaf),
                GlobalResponse(objects)
            )
        val routeCardData =
            RouteCardData(
                RouteCardData.LineOrRoute.Line(line, setOf(route)),
                listOf(routeStopData),
                RouteCardData.Context.StopDetailsFiltered,
                now
            )

        composeTestRule.setContent {
            StopDetailsFilteredDeparturesView(
                stopId = stop.id,
                stopFilter = StopDetailsFilter(route.id, 0),
                tripFilter = null,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                tileData = listOf(),
                noPredictionsStatus = UpcomingFormat.NoTripsFormat.ServiceEndedToday,
                allAlerts = null,
                elevatorAlerts = listOf(),
                data =
                    FilteredDeparturesData.PostGroupByDirection(
                        routeCardData = routeCardData,
                        routeStopData = routeStopData,
                        leaf = leaf
                    ),
                global = globalResponse,
                now = now,
                viewModel = StopDetailsViewModel.mocked(),
                errorBannerViewModel = errorBannerViewModel,
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {}
            )
        }

        composeTestRule.onNodeWithText("Service ended").assertIsDisplayed()
    }

    @Test
    fun testShowsSuspensionPreGroupByDirection(): Unit = runBlocking {
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
                            Alert.InformedEntity.Activity.Ride
                        ),
                    directionId = 0,
                    route = route.id,
                    stop = stop.id
                )
            }
        val alertResponse = AlertsStreamDataResponse(mapOf(alert.id to alert))
        val departures =
            checkNotNull(
                StopDetailsDepartures.fromData(
                    stop,
                    globalResponse,
                    null,
                    PredictionsStreamDataResponse(builder),
                    alertResponse,
                    emptySet(),
                    now,
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
                data =
                    FilteredDeparturesData.PreGroupByDirection(
                        patternsByStop = departures.routes.first { it.routeIdentifier == route.id }
                    ),
                tileData =
                    departures
                        .stopDetailsFormattedTrips(
                            filterState.value.routeId,
                            filterState.value.directionId,
                            now
                        )
                        .mapNotNull { TileData.fromUpcoming(it.upcoming, route, now) },
                allAlerts = null,
                elevatorAlerts = emptyList(),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {},
                noPredictionsStatus = null,
            )
        }

        composeTestRule.onNodeWithText("Service suspended", true).assertIsDisplayed()
        composeTestRule.onNodeWithText("View details").assertHasClickAction()
    }

    @Test
    fun testShowsSuspensionPostGroupByDirection(): Unit = runBlocking {
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
                            Alert.InformedEntity.Activity.Ride
                        ),
                    directionId = 0,
                    route = route.id,
                    stop = stop.id
                )
            }
        val alertResponse = AlertsStreamDataResponse(mapOf(alert.id to alert))

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
                    context = RouteCardData.Context.StopDetailsFiltered
                )
            )
        val routeStopData = routeCardData.single().stopData.single()
        val leaf = routeStopData.data.first { it.directionId == 0 }
        val leafFormat =
            leaf.format(now, route, globalResponse, RouteCardData.Context.StopDetailsFiltered)
        val viewModel =
            StopDetailsViewModel.mocked(
                settingsRepository =
                    MockSettingsRepository(mapOf(Settings.GroupByDirection to true))
            )
        viewModel.loadSettings()
        viewModel.setRouteCardData(routeCardData)

        composeTestRule.setContent {
            val filterState = remember {
                mutableStateOf(StopDetailsFilter(routeId = route.id, directionId = 0))
            }

            StopDetailsFilteredDeparturesView(
                stopId = stop.id,
                stopFilter = filterState.value,
                tripFilter = null,
                data =
                    FilteredDeparturesData.PostGroupByDirection(
                        routeCardData = routeCardData.single(),
                        routeStopData = routeStopData,
                        leaf = leaf
                    ),
                tileData = leafFormat.tileData(),
                allAlerts = null,
                elevatorAlerts = emptyList(),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {},
                noPredictionsStatus = leafFormat.noPredictionsStatus(),
            )
        }

        composeTestRule.onNodeWithText("Service suspended", true).assertIsDisplayed()
        composeTestRule.onNodeWithText("View details").assertHasClickAction()
    }

    @Test
    fun testShowsDownstreamAlertPreGroupByDirection(): Unit = runBlocking {
        val alert =
            builder.alert {
                activePeriod(now - 5.seconds, now + 5.seconds)
                effect = Alert.Effect.Suspension
                informedEntity(
                    activities =
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Exit,
                            Alert.InformedEntity.Activity.Ride
                        ),
                    directionId = 0,
                    route = route.id,
                    stop = downstreamStop.id
                )
            }
        val alertResponse = AlertsStreamDataResponse(mapOf(alert.id to alert))
        val departures =
            checkNotNull(
                StopDetailsDepartures.fromData(
                    stop,
                    globalResponse,
                    null,
                    PredictionsStreamDataResponse(builder),
                    alertResponse,
                    emptySet(),
                    now,
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
                data =
                    FilteredDeparturesData.PreGroupByDirection(
                        patternsByStop = departures.routes.first { it.routeIdentifier == route.id }
                    ),
                tileData =
                    departures
                        .stopDetailsFormattedTrips(
                            filterState.value.routeId,
                            filterState.value.directionId,
                            now
                        )
                        .mapNotNull { TileData.fromUpcoming(it.upcoming, route, now) },
                allAlerts = alertResponse,
                elevatorAlerts = emptyList(),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {},
                noPredictionsStatus = null,
            )
        }

        composeTestRule.onNodeWithText("Service suspended", true).assertIsDisplayed()
    }

    @Test
    fun testShowsDownstreamAlertPostGroupByDirection(): Unit = runBlocking {
        val alert =
            builder.alert {
                activePeriod(now - 5.seconds, now + 5.seconds)
                effect = Alert.Effect.Suspension
                informedEntity(
                    activities =
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Exit,
                            Alert.InformedEntity.Activity.Ride
                        ),
                    directionId = 0,
                    route = route.id,
                    stop = downstreamStop.id
                )
            }
        val alertResponse = AlertsStreamDataResponse(mapOf(alert.id to alert))

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
                    context = RouteCardData.Context.StopDetailsFiltered
                )
            )
        val routeStopData = routeCardData.single().stopData.single()
        val leaf = routeStopData.data.first { it.directionId == 0 }
        val leafFormat =
            leaf.format(now, route, globalResponse, RouteCardData.Context.StopDetailsFiltered)
        val viewModel =
            StopDetailsViewModel.mocked(
                settingsRepository =
                    MockSettingsRepository(mapOf(Settings.GroupByDirection to true))
            )
        viewModel.loadSettings()
        viewModel.setRouteCardData(routeCardData)

        composeTestRule.setContent {
            val filterState = remember {
                mutableStateOf(StopDetailsFilter(routeId = route.id, directionId = 0))
            }

            StopDetailsFilteredDeparturesView(
                stopId = stop.id,
                stopFilter = filterState.value,
                tripFilter = null,
                data =
                    FilteredDeparturesData.PostGroupByDirection(
                        routeCardData = routeCardData.single(),
                        routeStopData = routeStopData,
                        leaf = leaf
                    ),
                tileData = leafFormat.tileData(),
                allAlerts = alertResponse,
                elevatorAlerts = emptyList(),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {},
                noPredictionsStatus = leafFormat.noPredictionsStatus(),
            )
        }

        composeTestRule.onNodeWithText("Service suspended", true).assertIsDisplayed()
    }

    @Test
    fun testShowsElevatorAlertPreGroupByDirection(): Unit = runBlocking {
        val alert =
            builder.alert {
                effect = Alert.Effect.ElevatorClosure
                header = "Elevator Alert Header"
            }
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
                )
            )
        val settings =
            MockSettingsRepository(settings = mapOf(Settings.ElevatorAccessibility to true))
        val viewModel = StopDetailsViewModel.mocked(settingsRepository = settings)
        viewModel.setDepartures(departures)
        viewModel.loadSettings()

        composeTestRule.setContent {
            val filterState = remember {
                mutableStateOf(StopDetailsFilter(routeId = route.id, directionId = 0))
            }

            StopDetailsFilteredDeparturesView(
                stopId = stop.id,
                stopFilter = filterState.value,
                tripFilter = null,
                data =
                    FilteredDeparturesData.PreGroupByDirection(
                        patternsByStop = departures.routes.first { it.routeIdentifier == route.id }
                    ),
                tileData =
                    departures
                        .stopDetailsFormattedTrips(
                            filterState.value.routeId,
                            filterState.value.directionId,
                            now
                        )
                        .mapNotNull { TileData.fromUpcoming(it.upcoming, route, now) },
                allAlerts = null,
                elevatorAlerts = listOf(alert),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {},
                noPredictionsStatus = null,
            )
        }

        composeTestRule.onNodeWithText("Elevator Alert Header").assertIsDisplayed()
    }

    @Test
    fun testShowsElevatorAlertPostGroupByDirection(): Unit = runBlocking {
        val alert =
            builder.alert {
                effect = Alert.Effect.ElevatorClosure
                header = "Elevator Alert Header"
            }

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
                    context = RouteCardData.Context.StopDetailsFiltered
                )
            )
        val routeStopData = routeCardData.single().stopData.single()
        val leaf = routeStopData.data.first { it.directionId == 0 }
        val leafFormat =
            leaf.format(now, route, globalResponse, RouteCardData.Context.StopDetailsFiltered)
        val viewModel =
            StopDetailsViewModel.mocked(
                settingsRepository =
                    MockSettingsRepository(
                        mapOf(
                            Settings.GroupByDirection to true,
                            Settings.ElevatorAccessibility to true
                        )
                    )
            )
        viewModel.loadSettings()
        viewModel.setRouteCardData(routeCardData)

        composeTestRule.setContent {
            val filterState = remember {
                mutableStateOf(StopDetailsFilter(routeId = route.id, directionId = 0))
            }

            StopDetailsFilteredDeparturesView(
                stopId = stop.id,
                stopFilter = filterState.value,
                tripFilter = null,
                data =
                    FilteredDeparturesData.PostGroupByDirection(
                        routeCardData = routeCardData.single(),
                        routeStopData = routeStopData,
                        leaf = leaf
                    ),
                tileData = leafFormat.tileData(),
                allAlerts = null,
                elevatorAlerts = listOf(alert),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {},
                noPredictionsStatus = leafFormat.noPredictionsStatus(),
            )
        }

        composeTestRule.onNodeWithText("Elevator Alert Header").assertIsDisplayed()
    }

    @Test
    fun testShowsSubwayDelayAlertPreGroupByDirection(): Unit = runBlocking {
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
                            Alert.InformedEntity.Activity.Ride
                        ),
                    directionId = 0,
                    route = route.id,
                    routeType = RouteType.LIGHT_RAIL,
                    stop = stop.id
                )
            }
        val alertResponse = AlertsStreamDataResponse(mapOf(alert.id to alert))
        val departures =
            checkNotNull(
                StopDetailsDepartures.fromData(
                    stop,
                    globalResponse,
                    null,
                    PredictionsStreamDataResponse(builder),
                    alertResponse,
                    emptySet(),
                    now,
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
                data =
                    FilteredDeparturesData.PreGroupByDirection(
                        patternsByStop = departures.routes.first { it.routeIdentifier == route.id }
                    ),
                tileData =
                    departures
                        .stopDetailsFormattedTrips(
                            filterState.value.routeId,
                            filterState.value.directionId,
                            now
                        )
                        .mapNotNull { TileData.fromUpcoming(it.upcoming, route, now) },
                allAlerts = null,
                elevatorAlerts = emptyList(),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {},
                noPredictionsStatus = null,
            )
        }

        composeTestRule.onNodeWithText("Delays due to heavy ridership").assertIsDisplayed()
    }

    @Test
    fun testShowsSubwayDelayAlertPostGroupByDirection(): Unit = runBlocking {
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
                            Alert.InformedEntity.Activity.Ride
                        ),
                    directionId = 0,
                    route = route.id,
                    routeType = RouteType.LIGHT_RAIL,
                    stop = stop.id
                )
            }
        val alertResponse = AlertsStreamDataResponse(mapOf(alert.id to alert))
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
                    context = RouteCardData.Context.StopDetailsFiltered
                )
            )
        val routeStopData = routeCardData.single().stopData.single()
        val leaf = routeStopData.data.first { it.directionId == 0 }
        val leafFormat =
            leaf.format(now, route, globalResponse, RouteCardData.Context.StopDetailsFiltered)
        val viewModel =
            StopDetailsViewModel.mocked(
                settingsRepository =
                    MockSettingsRepository(mapOf(Settings.GroupByDirection to true))
            )
        viewModel.loadSettings()
        viewModel.setRouteCardData(routeCardData)

        composeTestRule.setContent {
            val filterState = remember {
                mutableStateOf(StopDetailsFilter(routeId = route.id, directionId = 0))
            }

            StopDetailsFilteredDeparturesView(
                stopId = stop.id,
                stopFilter = filterState.value,
                tripFilter = null,
                data =
                    FilteredDeparturesData.PostGroupByDirection(
                        routeCardData = routeCardData.single(),
                        routeStopData = routeStopData,
                        leaf = leaf
                    ),
                tileData = leafFormat.tileData(),
                allAlerts = null,
                elevatorAlerts = emptyList(),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {},
                noPredictionsStatus = leafFormat.noPredictionsStatus(),
            )
        }

        composeTestRule.onNodeWithText("Delays due to heavy ridership").assertIsDisplayed()
    }

    @Test
    fun testShowsNotAccessibleAlertPreGroupByDirection(): Unit = runBlocking {
        val departures =
            checkNotNull(
                StopDetailsDepartures.fromData(
                    inaccessibleStop,
                    globalResponse,
                    null,
                    PredictionsStreamDataResponse(builder),
                    AlertsStreamDataResponse(emptyMap()),
                    emptySet(),
                    now,
                )
            )
        val settings =
            MockSettingsRepository(settings = mapOf(Settings.ElevatorAccessibility to true))
        val viewModel = StopDetailsViewModel.mocked(settingsRepository = settings)
        viewModel.setDepartures(departures)
        viewModel.loadSettings()

        composeTestRule.setContent {
            val filterState = remember {
                mutableStateOf(StopDetailsFilter(routeId = route.id, directionId = 0))
            }

            StopDetailsFilteredDeparturesView(
                stopId = inaccessibleStop.id,
                stopFilter = filterState.value,
                tripFilter = null,
                data =
                    FilteredDeparturesData.PreGroupByDirection(
                        patternsByStop = departures.routes.first { it.routeIdentifier == route.id }
                    ),
                tileData =
                    departures
                        .stopDetailsFormattedTrips(
                            filterState.value.routeId,
                            filterState.value.directionId,
                            now
                        )
                        .mapNotNull { TileData.fromUpcoming(it.upcoming, route, now) },
                allAlerts = null,
                elevatorAlerts = emptyList(),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {},
                noPredictionsStatus = null,
            )
        }

        composeTestRule.onNodeWithText("This stop is not accessible").assertIsDisplayed()
    }

    @Test
    fun testShowsNotAccessibleAlertPostGroupByDirection(): Unit = runBlocking {
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
                    RouteCardData.Context.StopDetailsFiltered
                )
            )
        val routeStopData = routeCardData.single().stopData.single()
        val leaf = routeStopData.data.first { it.directionId == 0 }
        val leafFormat =
            leaf.format(now, route, globalResponse, RouteCardData.Context.StopDetailsFiltered)
        val viewModel =
            StopDetailsViewModel.mocked(
                settingsRepository =
                    MockSettingsRepository(
                        mapOf(
                            Settings.GroupByDirection to true,
                            Settings.ElevatorAccessibility to true
                        )
                    )
            )
        viewModel.loadSettings()
        viewModel.setRouteCardData(routeCardData)

        composeTestRule.setContent {
            val filterState = remember {
                mutableStateOf(StopDetailsFilter(routeId = route.id, directionId = 0))
            }

            StopDetailsFilteredDeparturesView(
                stopId = inaccessibleStop.id,
                stopFilter = filterState.value,
                tripFilter = null,
                data =
                    FilteredDeparturesData.PostGroupByDirection(
                        routeCardData = routeCardData.single(),
                        routeStopData = routeStopData,
                        leaf = leaf
                    ),
                tileData = leafFormat.tileData(),
                allAlerts = null,
                elevatorAlerts = emptyList(),
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = {},
                openModal = {},
                openSheetRoute = {},
                noPredictionsStatus = leafFormat.noPredictionsStatus(),
            )
        }

        composeTestRule.onNodeWithText("This stop is not accessible").assertIsDisplayed()
    }
}
