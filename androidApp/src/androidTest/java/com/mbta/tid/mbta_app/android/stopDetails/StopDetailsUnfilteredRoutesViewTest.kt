package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.ErrorBannerViewModel
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext

class StopDetailsUnfilteredRoutesViewTest {
    val builder = ObjectCollectionBuilder()
    val now = EasternTimeInstant.now()
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
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
        }
    val inaccessibleStop =
        builder.stop {
            id = "stop_2"
            name = "Inaccessible Stop"
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

    val globalResponse =
        GlobalResponse(
            builder,
            mutableMapOf(stop.id to listOf(routePatternOne.id, routePatternTwo.id)),
        )

    private val errorBannerViewModel = ErrorBannerViewModel(MockErrorBannerStateRepository())

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testGroupsByDirection() = runBlocking {
        val routeCardData =
            checkNotNull(
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id) + stop.childStopIds,
                    globalResponse,
                    null,
                    null,
                    PredictionsStreamDataResponse(builder),
                    AlertsStreamDataResponse(emptyMap()),
                    now,
                    RouteCardData.Context.StopDetailsUnfiltered,
                )
            )

        val koin = testKoinApplication {
            settings = MockSettingsRepository(mapOf(Settings.StationAccessibility to true))
        }
        composeTestRule.setContent {
            val filterState = remember { mutableStateOf<StopDetailsFilter?>(null) }

            KoinContext(koin.koin) {
                StopDetailsUnfilteredRoutesView(
                    stop = stop,
                    routeCardData = routeCardData,
                    servedRoutes = emptyList(),
                    errorBannerViewModel = errorBannerViewModel,
                    now = now,
                    globalData = globalResponse,
                    isFavorite = { false },
                    onClose = {},
                    onTapRoutePill = {},
                    updateStopFilter = filterState::value::set,
                    openModal = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Sample Route").assertExists()
        composeTestRule.onNodeWithText("Sample Headsign").assertExists()
        composeTestRule.onNodeWithText("1 min").assertExists()
        composeTestRule.onNodeWithText("This stop is not accessible").assertDoesNotExist()
    }

    @Test
    fun testInaccessibleByDirection(): Unit = runBlocking {
        val routeCardData =
            checkNotNull(
                RouteCardData.routeCardsForStopList(
                    listOf(inaccessibleStop.id) + inaccessibleStop.childStopIds,
                    globalResponse,
                    null,
                    null,
                    PredictionsStreamDataResponse(builder),
                    AlertsStreamDataResponse(emptyMap()),
                    now,
                    RouteCardData.Context.StopDetailsUnfiltered,
                )
            )

        val koin = testKoinApplication {
            settings = MockSettingsRepository(mapOf(Settings.StationAccessibility to true))
        }
        composeTestRule.setContent {
            val filterState = remember { mutableStateOf<StopDetailsFilter?>(null) }

            KoinContext(koin.koin) {
                StopDetailsUnfilteredRoutesView(
                    stop = inaccessibleStop,
                    routeCardData = routeCardData,
                    servedRoutes = emptyList(),
                    errorBannerViewModel = errorBannerViewModel,
                    now = now,
                    globalData = globalResponse,
                    isFavorite = { false },
                    onClose = {},
                    onTapRoutePill = {},
                    updateStopFilter = filterState::value::set,
                    openModal = {},
                )
            }
        }

        composeTestRule.onNodeWithText("This stop is not accessible").assertExists()
    }

    @Test
    fun testShowsElevatorAlertsWhenGroupedByDirection(): Unit = runBlocking {
        val alert =
            Single.alert {
                header = "Elevator alert"
                activePeriod(start = EasternTimeInstant(Instant.DISTANT_PAST), end = null)
                effect = Alert.Effect.ElevatorClosure
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.UsingWheelchair),
                    stop = stop.id,
                )
            }
        val routeCardData =
            checkNotNull(
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id) + stop.childStopIds,
                    globalResponse,
                    null,
                    null,
                    PredictionsStreamDataResponse(builder),
                    AlertsStreamDataResponse(mapOf(alert.id to alert)),
                    now,
                    RouteCardData.Context.StopDetailsUnfiltered,
                )
            )

        val koin = testKoinApplication {
            settings = MockSettingsRepository(mapOf(Settings.StationAccessibility to true))
        }
        composeTestRule.setContent {
            val filterState = remember { mutableStateOf<StopDetailsFilter?>(null) }

            KoinContext(koin.koin) {
                StopDetailsUnfilteredRoutesView(
                    stop = stop,
                    routeCardData = routeCardData,
                    servedRoutes = emptyList(),
                    errorBannerViewModel = errorBannerViewModel,
                    now = now,
                    globalData = globalResponse,
                    isFavorite = { false },
                    onClose = {},
                    onTapRoutePill = {},
                    updateStopFilter = filterState::value::set,
                    openModal = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Elevator alert").assertIsDisplayed()
    }
}
