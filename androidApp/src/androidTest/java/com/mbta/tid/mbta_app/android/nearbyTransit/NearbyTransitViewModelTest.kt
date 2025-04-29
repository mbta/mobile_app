package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import io.github.dellisd.spatialk.geojson.Position
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.fail
import org.junit.Rule
import org.junit.Test

class NearbyTransitViewModelTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testNearby() {
        val objects = ObjectCollectionBuilder()

        val route = objects.route()
        val stop1 = objects.stop()
        objects.routePattern(route) { representativeTrip { stopIds = listOf(stop1.id) } }
        val stop2 = objects.stop()
        objects.routePattern(route) { representativeTrip { stopIds = listOf(stop2.id) } }

        val globalResponse = GlobalResponse(objects)

        val position1 = Position(0.0, 0.0)
        val position2 = Position(1.0, 1.0)

        val response1 = NearbyStaticData(globalResponse, NearbyResponse(listOf(stop1.id)))
        val response2 = NearbyStaticData(globalResponse, NearbyResponse(listOf(stop2.id)))
        assertNotEquals(response1, response2, "not actually testing anything")

        val nearbyRepository =
            object : INearbyRepository {
                override fun getStopIdsNearby(
                    global: GlobalResponse,
                    location: Position
                ): List<String> = emptyList()

                override suspend fun getNearby(
                    global: GlobalResponse,
                    stopIds: List<String>
                ): ApiResult<NearbyStaticData> {
                    fail("getNearby should be called with location")
                }

                override suspend fun getNearby(
                    global: GlobalResponse,
                    location: Position
                ): ApiResult<NearbyStaticData> {
                    return if (location === position1) {
                        ApiResult.Ok(response1)
                    } else {
                        ApiResult.Ok(response2)
                    }
                }
            }

        var position by mutableStateOf(position1)
        val nearbyVM =
            NearbyTransitViewModel(
                nearbyRepository,
                errorBannerRepository = MockErrorBannerStateRepository(),
                analytics = MockAnalytics()
            )

        composeTestRule.setContent {
            LaunchedEffect(position) {
                nearbyVM.getNearby(
                    globalResponse,
                    position,
                    setLastLocation = {},
                    setSelectingLocation = {}
                )
            }
        }

        composeTestRule.waitUntil { nearbyVM.nearby != null }
        assertEquals(response1, nearbyVM.nearby)

        position = position2
        composeTestRule.waitUntil { nearbyVM.nearby != response1 }
        assertEquals(response2, nearbyVM.nearby)
    }
}
