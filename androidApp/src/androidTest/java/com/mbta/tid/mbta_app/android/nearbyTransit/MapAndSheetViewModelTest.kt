package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.maplibre.spatialk.geojson.Position

class MapAndSheetViewModelTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testNearbyStopIds() {
        val objects = ObjectCollectionBuilder()

        val route = objects.route()
        val stop1 = objects.stop()
        objects.routePattern(route) { representativeTrip { stopIds = listOf(stop1.id) } }
        val stop2 = objects.stop()
        objects.routePattern(route) { representativeTrip { stopIds = listOf(stop2.id) } }

        val globalResponse = GlobalResponse(objects)

        val position1 = Position(0.0, 0.0)
        val position2 = Position(1.0, 1.0)

        val response1 = listOf(stop1.id)
        val response2 = listOf(stop2.id)
        assertNotEquals(response1, response2, "not actually testing anything")

        val nearbyRepository =
            object : INearbyRepository {
                override fun getStopIdsNearby(
                    global: GlobalResponse,
                    location: Position,
                ): List<String> {
                    return if (location === position1) {
                        response1
                    } else {
                        response2
                    }
                }
            }

        var position by mutableStateOf(position1)
        val nearbyVM = NearbyTransitViewModel(nearbyRepository)

        composeTestRule.setContent {
            LaunchedEffect(position) {
                nearbyVM.getNearby(
                    globalResponse,
                    position,
                    setLastLocation = {},
                    setIsTargeting = {},
                )
            }
        }

        composeTestRule.waitUntilDefaultTimeout { nearbyVM.nearbyStopIds != null }
        assertEquals(response1, nearbyVM.nearbyStopIds)

        position = position2
        composeTestRule.waitUntilDefaultTimeout { nearbyVM.nearbyStopIds != response1 }
        assertEquals(response2, nearbyVM.nearbyStopIds)
    }
}
