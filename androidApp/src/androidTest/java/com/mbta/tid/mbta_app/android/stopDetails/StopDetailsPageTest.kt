package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.android.MainApplication
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockScheduleRepository
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test
import org.koin.dsl.koinApplication

class StopDetailsPageTest {

    @get:Rule val composeTestRule = createComposeRule()

    val koinApplication = koinApplication {
        modules(
            repositoriesModule(MockRepositories.buildWithDefaults()),
            MainApplication.koinViewModelModule
        )
    }

    @Test
    fun testAppliesStopFilterAutomatically() = runTest {
        val filters =
            mutableStateOf<StopDetailsPageFilters?>(StopDetailsPageFilters("stop_1", null, null))

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

        composeTestRule.setContent {
            var filters by remember { filters }

            /*       StopDetailsPage(
                          viewModel,
                          filters,




                      )

            */

        }
    }

    @Test fun testAppliesTripFilterAutomatically() {}

    @Test fun testAppliesTripFilterWhenStopFilterChanged() {}
}
