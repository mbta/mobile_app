package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.model.Coordinate
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GetNearbyTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testNearby() = runTest {
        val builder1 = ObjectCollectionBuilder()

        val globalResponse = GlobalResponse(builder1)

        val builder2 = ObjectCollectionBuilder()

        val coordinate1 = Coordinate(0.0, 0.0)
        val coordinate2 = Coordinate(1.0, 1.0)

        val nearbyRepository =
            object : INearbyRepository {
                override suspend fun getNearby(
                    globalResponse: GlobalResponse,
                    location: Coordinate
                ): ApiResult<NearbyStaticData> {
                    if (location == coordinate1) {
                        return ApiResult.Ok(
                            NearbyStaticData(globalResponse, NearbyResponse(builder1))
                        )
                    } else {
                        return ApiResult.Ok(
                            NearbyStaticData(globalResponse, NearbyResponse(builder2))
                        )
                    }
                }
            }

        var coordinate by mutableStateOf(coordinate1)
        var actualNearby: NearbyStaticData? = null

        composeTestRule.setContent {
            actualNearby =
                getNearby(
                    globalResponse = globalResponse,
                    location = coordinate1,
                    setLastLocation = { /* null-op */},
                    setSelectingLocation = {},
                    nearbyRepository = nearbyRepository
                )
        }

        composeTestRule.awaitIdle()
        assertEquals(NearbyStaticData(globalResponse, NearbyResponse(builder1)), actualNearby)

        coordinate = coordinate2
        composeTestRule.awaitIdle()
        assertEquals(NearbyStaticData(globalResponse, NearbyResponse(builder2)), actualNearby)
    }
}
