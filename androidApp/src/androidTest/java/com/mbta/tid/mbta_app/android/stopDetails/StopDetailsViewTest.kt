package com.mbta.tid.mbta_app.android.stopDetails

// TODO: Convert to use RouteCardData or remove these
class StopDetailsViewTest {
    //    val builder = ObjectCollectionBuilder()
    //    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    //    val route =
    //        builder.route {
    //            id = "route_1"
    //            type = RouteType.BUS
    //            color = "FF0000"
    //            directionNames = listOf("North", "South")
    //            directionDestinations = listOf("Downtown", "Uptown")
    //            longName = "Sample Route Long Name"
    //            shortName = "Sample Route"
    //            textColor = "000000"
    //            lineId = "line_1"
    //            routePatternIds = mutableListOf("pattern_1", "pattern_2")
    //        }
    //    val routePatternOne =
    //        builder.routePattern(route) {
    //            id = "pattern_1"
    //            directionId = 0
    //            name = "Sample Route Pattern"
    //            routeId = "route_1"
    //            representativeTripId = "trip_1"
    //        }
    //    val routePatternTwo =
    //        builder.routePattern(route) {
    //            id = "pattern_2"
    //            directionId = 1
    //            name = "Sample Route Pattern Two"
    //            routeId = "route_1"
    //            representativeTripId = "trip_1"
    //        }
    //    val stop =
    //        builder.stop {
    //            id = "stop_1"
    //            name = "Sample Stop"
    //            locationType = LocationType.STOP
    //            latitude = 0.0
    //            longitude = 0.0
    //        }
    //    val line =
    //        builder.line {
    //            id = "line_1"
    //            color = "FF0000"
    //            textColor = "FFFFFF"
    //        }
    //    val trip =
    //        builder.trip {
    //            id = "trip_1"
    //            routeId = "route_1"
    //            directionId = 0
    //            headsign = "Sample Headsign"
    //            stopIds = listOf(stop.id)
    //        }
    //    val prediction =
    //        builder.prediction {
    //            id = "prediction_1"
    //            revenue = true
    //            stopId = "stop_1"
    //            tripId = "trip_1"
    //            routeId = "route_1"
    //            stopSequence = 1
    //            directionId = 0
    //            arrivalTime = now.plus(1.minutes)
    //            departureTime = now.plus(1.5.minutes)
    //        }
    //
    //    val settingsRepository =
    //        MockSettingsRepository(settings = mapOf(Pair(Settings.StationAccessibility, true)))
    //
    //    val koinApplication = testKoinApplication(builder) { settings = settingsRepository }
    //
    //    @get:Rule val composeTestRule = createComposeRule()
    //
    //    @OptIn(ExperimentalTestApi::class)
    //    @Test
    //    fun testStopDetailsViewDisplaysUnfilteredCorrectly() {
    //        val viewModel = StopDetailsViewModel.mocked()
    //
    //        viewModel.setDepartures(
    //            StopDetailsDepartures(
    //                listOf(
    //                    PatternsByStop(
    //                        route = route,
    //                        stop = stop,
    //                        patterns =
    //                            listOf(
    //                                RealtimePatterns.ByHeadsign(
    //                                    staticData =
    //                                        NearbyStaticData.StaticPatterns.ByHeadsign(
    //                                            route = route,
    //                                            headsign = trip.headsign,
    //                                            line = line,
    //                                            patterns = listOf(routePatternOne,
    // routePatternTwo),
    //                                            stopIds = setOf(stop.id),
    //                                        ),
    //                                    upcomingTripsMap =
    //                                        mapOf(
    //                                            RealtimePatterns.UpcomingTripKey.ByRoutePattern(
    //                                                trip.routeId,
    //                                                routePatternOne.id,
    //                                                stop.id
    //                                            ) to listOf(UpcomingTrip(trip, prediction))
    //                                        ),
    //                                    parentStopId = stop.id,
    //                                    alertsHere = emptyList(),
    //                                    alertsDownstream = emptyList(),
    //                                    hasSchedulesTodayByPattern = null,
    //                                    allDataLoaded = false
    //                                )
    //                            )
    //                    )
    //                )
    //            )
    //        )
    //        composeTestRule.setContent {
    //            KoinContext(koinApplication.koin) {
    //                val filterState = remember { mutableStateOf<StopDetailsFilter?>(null) }
    //                StopDetailsView(
    //                    stopId = stop.id,
    //                    viewModel = viewModel,
    //                    pinnedRoutes = emptySet(),
    //                    togglePinnedRoute = {},
    //                    onClose = {},
    //                    stopFilter = null,
    //                    tripFilter = null,
    //                    allAlerts = null,
    //                    updateStopFilter = filterState::value::set,
    //                    updateTripDetailsFilter = {},
    //                    tileScrollState = rememberScrollState(),
    //                    errorBannerViewModel =
    //                        ErrorBannerViewModel(
    //                            false,
    //                            MockErrorBannerStateRepository(),
    //                        ),
    //                    openModal = {},
    //                    openSheetRoute = {}
    //                )
    //            }
    //        }
    //
    //        composeTestRule.waitUntilExactlyOneExists(hasText("Sample Stop"))
    //
    //        composeTestRule.onNode(hasText("Sample Stop") and isHeading()).assertIsDisplayed()
    //
    //        composeTestRule.onNodeWithText("Sample Route").assertExists()
    //        composeTestRule.onNodeWithText("Sample Headsign").assertExists()
    //        composeTestRule.onNodeWithText("1 min").assertExists()
    //    }
    //
    //    @OptIn(ExperimentalTestApi::class)
    //    @Test
    //    fun testStopDetailsViewDisplaysFilteredCorrectly() {
    //        val viewModel = StopDetailsViewModel.mocked()
    //
    //        viewModel.setDepartures(
    //            StopDetailsDepartures(
    //                listOf(
    //                    PatternsByStop(
    //                        route = route,
    //                        stop = stop,
    //                        patterns =
    //                            listOf(
    //                                RealtimePatterns.ByHeadsign(
    //                                    staticData =
    //                                        NearbyStaticData.StaticPatterns.ByHeadsign(
    //                                            route = route,
    //                                            headsign = trip.headsign,
    //                                            line = line,
    //                                            patterns = listOf(routePatternOne,
    // routePatternTwo),
    //                                            stopIds = setOf(stop.id),
    //                                        ),
    //                                    upcomingTripsMap =
    //                                        mapOf(
    //                                            RealtimePatterns.UpcomingTripKey.ByRoutePattern(
    //                                                trip.routeId,
    //                                                routePatternOne.id,
    //                                                stop.id
    //                                            ) to listOf(UpcomingTrip(trip, prediction))
    //                                        ),
    //                                    parentStopId = stop.id,
    //                                    alertsHere = emptyList(),
    //                                    alertsDownstream = emptyList(),
    //                                    hasSchedulesTodayByPattern = null,
    //                                    allDataLoaded = false
    //                                )
    //                            )
    //                    )
    //                )
    //            )
    //        )
    //        composeTestRule.setContent {
    //            KoinContext(koinApplication.koin) {
    //                val filterState = remember {
    //                    mutableStateOf<StopDetailsFilter?>(StopDetailsFilter(route.id, 0))
    //                }
    //                StopDetailsView(
    //                    stopId = stop.id,
    //                    viewModel = viewModel,
    //                    pinnedRoutes = emptySet(),
    //                    togglePinnedRoute = {},
    //                    onClose = {},
    //                    stopFilter = filterState.value,
    //                    tripFilter = null,
    //                    allAlerts = null,
    //                    updateStopFilter = filterState::value::set,
    //                    updateTripDetailsFilter = {},
    //                    tileScrollState = rememberScrollState(),
    //                    errorBannerViewModel =
    //                        ErrorBannerViewModel(
    //                            false,
    //                            MockErrorBannerStateRepository(),
    //                        ),
    //                    openModal = {},
    //                    openSheetRoute = {}
    //                )
    //            }
    //        }
    //
    //        composeTestRule.waitUntilExactlyOneExists(hasText("at Sample Stop"))
    //
    //        composeTestRule.onNodeWithContentDescription("Star route").assertExists()
    //        composeTestRule.onNodeWithContentDescription("Close").assertExists()
    //        composeTestRule.onNode(hasText("at Sample Stop") and isHeading()).assertIsDisplayed()
    //
    //        composeTestRule.onNodeWithText("1 min").assertExists()
    //    }
    //
    //    @Test
    //    fun testStopDetailsViewDisplaysElevatorAlertsOnUnfiltered() {
    //        val alert =
    //            builder.alert {
    //                effect = Alert.Effect.ElevatorClosure
    //                header = "Elevator alert header"
    //            }
    //
    //        val viewModel = StopDetailsViewModel.mocked()
    //
    //        viewModel.setDepartures(
    //            StopDetailsDepartures(
    //                listOf(
    //                    PatternsByStop(
    //                        route = route,
    //                        stop = stop,
    //                        patterns =
    //                            listOf(
    //                                RealtimePatterns.ByHeadsign(
    //                                    staticData =
    //                                        NearbyStaticData.StaticPatterns.ByHeadsign(
    //                                            route = route,
    //                                            headsign = trip.headsign,
    //                                            line = line,
    //                                            patterns = listOf(routePatternOne,
    // routePatternTwo),
    //                                            stopIds = setOf(stop.id),
    //                                        ),
    //                                    upcomingTripsMap =
    //                                        mapOf(
    //                                            RealtimePatterns.UpcomingTripKey.ByRoutePattern(
    //                                                trip.routeId,
    //                                                routePatternOne.id,
    //                                                stop.id
    //                                            ) to listOf(UpcomingTrip(trip, prediction))
    //                                        ),
    //                                    parentStopId = stop.id,
    //                                    alertsHere = emptyList(),
    //                                    alertsDownstream = emptyList(),
    //                                    hasSchedulesTodayByPattern = null,
    //                                    allDataLoaded = false
    //                                )
    //                            ),
    //                        listOf(alert)
    //                    )
    //                )
    //            )
    //        )
    //        composeTestRule.setContent {
    //            KoinContext(koinApplication.koin) {
    //                val filterState = remember { mutableStateOf<StopDetailsFilter?>(null) }
    //                StopDetailsView(
    //                    stopId = stop.id,
    //                    viewModel = viewModel,
    //                    pinnedRoutes = emptySet(),
    //                    togglePinnedRoute = {},
    //                    onClose = {},
    //                    stopFilter = filterState.value,
    //                    tripFilter = null,
    //                    allAlerts = null,
    //                    updateStopFilter = filterState::value::set,
    //                    updateTripDetailsFilter = {},
    //                    tileScrollState = rememberScrollState(),
    //                    errorBannerViewModel =
    //                        ErrorBannerViewModel(
    //                            false,
    //                            MockErrorBannerStateRepository(),
    //                        ),
    //                    openModal = {},
    //                    openSheetRoute = {}
    //                )
    //            }
    //        }
    //
    //        composeTestRule.onNodeWithText(alert.header!!).assertExists()
    //    }
    //
    //    @Test
    //    fun testStopDetailsViewDisplaysElevatorAlertsOnFiltered() {
    //        val alert =
    //            builder.alert {
    //                effect = Alert.Effect.ElevatorClosure
    //                header = "Elevator alert header"
    //            }
    //
    //        val viewModel = StopDetailsViewModel.mocked()
    //
    //        viewModel.setDepartures(
    //            StopDetailsDepartures(
    //                listOf(
    //                    PatternsByStop(
    //                        route = route,
    //                        stop = stop,
    //                        patterns =
    //                            listOf(
    //                                RealtimePatterns.ByHeadsign(
    //                                    staticData =
    //                                        NearbyStaticData.StaticPatterns.ByHeadsign(
    //                                            route = route,
    //                                            headsign = trip.headsign,
    //                                            line = line,
    //                                            patterns = listOf(routePatternOne,
    // routePatternTwo),
    //                                            stopIds = setOf(stop.id),
    //                                        ),
    //                                    upcomingTripsMap =
    //                                        mapOf(
    //                                            RealtimePatterns.UpcomingTripKey.ByRoutePattern(
    //                                                trip.routeId,
    //                                                routePatternOne.id,
    //                                                stop.id
    //                                            ) to listOf(UpcomingTrip(trip, prediction))
    //                                        ),
    //                                    parentStopId = stop.id,
    //                                    alertsHere = emptyList(),
    //                                    alertsDownstream = emptyList(),
    //                                    hasSchedulesTodayByPattern = null,
    //                                    allDataLoaded = false
    //                                )
    //                            ),
    //                        listOf(alert)
    //                    )
    //                )
    //            )
    //        )
    //        composeTestRule.setContent {
    //            KoinContext(koinApplication.koin) {
    //                val filterState = remember {
    //                    mutableStateOf<StopDetailsFilter?>(StopDetailsFilter(route.id, 0))
    //                }
    //                StopDetailsView(
    //                    stopId = stop.id,
    //                    viewModel = viewModel,
    //                    pinnedRoutes = emptySet(),
    //                    togglePinnedRoute = {},
    //                    onClose = {},
    //                    stopFilter = filterState.value,
    //                    tripFilter = null,
    //                    allAlerts = null,
    //                    updateStopFilter = filterState::value::set,
    //                    updateTripDetailsFilter = {},
    //                    tileScrollState = rememberScrollState(),
    //                    errorBannerViewModel =
    //                        ErrorBannerViewModel(
    //                            false,
    //                            MockErrorBannerStateRepository(),
    //                        ),
    //                    openModal = {},
    //                    openSheetRoute = {}
    //                )
    //            }
    //        }
    //
    //        composeTestRule.onNodeWithText(alert.header!!).assertExists()
    //    }
}
