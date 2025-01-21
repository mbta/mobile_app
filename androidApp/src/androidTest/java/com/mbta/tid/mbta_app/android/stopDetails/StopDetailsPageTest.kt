package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.MainApplication
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.pages.StopDetailsPage
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockScheduleRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.KoinTest

class StopDetailsPageTest : KoinTest {

    @get:Rule val composeTestRule = createComposeRule()

    val koinApplication = koinApplication {
        modules(
            module { single<Analytics> { MockAnalytics() } },
            repositoriesModule(MockRepositories.buildWithDefaults()),
            MainApplication.koinViewModelModule
        )
    }

    @Test
    fun testAppliesStopFilterAutomatically() = runTest {
        val objects = ObjectCollectionBuilder()

        val now = Clock.System.now()

        val route = objects.route {}
        val stop = objects.stop {}

        val tripId = "trip"
        val routePattern = objects.routePattern(route) { representativeTripId = tripId }
        val trip1 =
            objects.trip(routePattern) {
                id = tripId
                directionId = 0
                stopIds = listOf(stop.id)
                routePatternId = routePattern.id
            }
        objects.schedule {
            routeId = route.id
            stopId = stop.id
            stopSequence = 0
            departureTime = now.plus(10.minutes)
            trip = trip1
        }

        val viewModel =
            StopDetailsViewModel(
                MockScheduleRepository(),
                MockPredictionsRepository(),
                MockErrorBannerStateRepository()
            )

        viewModel.setDepartures(
            StopDetailsDepartures.fromData(
                stop,
                GlobalResponse(objects),
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                setOf(),
                now,
                false
            )
        )

        val filters = mutableStateOf(StopDetailsPageFilters(stop.id, null, null))

        var newStopFilter: StopDetailsFilter? = null

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                var filters by remember { filters }
                val errorBannerVM =
                    ErrorBannerViewModel(
                        false,
                        MockErrorBannerStateRepository(),
                        MockSettingsRepository()
                    )

                StopDetailsPage(
                    viewModel = viewModel,
                    filters = filters,
                    now = now,
                    onClose = {},
                    updateStopFilter = { newStopFilter = it },
                    updateTripFilter = {},
                    updateDepartures = {},
                    errorBannerViewModel = errorBannerVM
                )
            }
        }

        composeTestRule.waitUntil {
            newStopFilter == StopDetailsFilter(route.id, routePattern.directionId)
        }

        assertEquals(StopDetailsFilter(route.id, routePattern.directionId), newStopFilter)
    }

    @Test
    fun testAppliesTripFilterAutomatically() = runTest {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}

        val now = Clock.System.now()

        val route = objects.route {}

        val tripId = "trip"
        val routePattern = objects.routePattern(route) { representativeTripId = tripId }
        val trip1 =
            objects.trip(routePattern) {
                id = tripId
                directionId = 0
                stopIds = listOf(stop.id)
                routePatternId = routePattern.id
            }
        val schedule =
            objects.schedule {
                routeId = route.id
                stopId = stop.id
                stopSequence = 0
                departureTime = now.plus(10.minutes)
                trip = trip1
            }

        objects.prediction(schedule) { departureTime = now.plus(10.minutes) }

        val viewModel =
            StopDetailsViewModel(
                MockScheduleRepository(),
                MockPredictionsRepository(),
                MockErrorBannerStateRepository()
            )

        viewModel.setDepartures(
            StopDetailsDepartures.fromData(
                stop,
                GlobalResponse(objects),
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                setOf(),
                now,
                false
            )
        )

        val filters =
            mutableStateOf(StopDetailsPageFilters(stop.id, StopDetailsFilter(route.id, 0), null))

        var newTripFilter: TripDetailsFilter? = null

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                var filters by remember { filters }
                val errorBannerVM =
                    ErrorBannerViewModel(
                        false,
                        MockErrorBannerStateRepository(),
                        MockSettingsRepository()
                    )

                StopDetailsPage(
                    viewModel = viewModel,
                    filters = filters,
                    now = now,
                    onClose = {},
                    updateStopFilter = {},
                    updateTripFilter = { newTripFilter = it },
                    updateDepartures = {},
                    errorBannerViewModel = errorBannerVM
                )
            }
        }

        val expectedTripFilter = TripDetailsFilter(trip1.id, null, 0, false)

        composeTestRule.waitUntil { newTripFilter == expectedTripFilter }
        assertEquals(expectedTripFilter, newTripFilter)
    }
}
