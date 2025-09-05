package com.mbta.tid.mbta_app.android.state

import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.SegmentedRouteShape
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.repositories.IRailRouteShapeRepository
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

class GetRailRouteShapesTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testRailRouteShapes() {
        val builder = ObjectCollectionBuilder()
        val route = builder.route()
        val routePattern = builder.routePattern(route)
        val mapFriendlyRouteResponse =
            MapFriendlyRouteResponse(
                listOf(
                    MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                        route.id,
                        listOf(
                            SegmentedRouteShape(
                                routePattern.id,
                                route.id,
                                1,
                                listOf(),
                                builder.shape(),
                            )
                        ),
                    )
                )
            )
        val railRouteShapeRepository =
            object : IRailRouteShapeRepository {
                override val state: StateFlow<MapFriendlyRouteResponse?>
                    get() {
                        /* null-op */
                        return MutableStateFlow<MapFriendlyRouteResponse?>(null)
                    }

                override suspend fun getRailRouteShapes(): ApiResult<MapFriendlyRouteResponse> {
                    return ApiResult.Ok(mapFriendlyRouteResponse)
                }
            }

        var actualRailRouteShapes: MapFriendlyRouteResponse? = null
        composeTestRule.setContent {
            actualRailRouteShapes = getRailRouteShapes(railRouteShapeRepository)
        }

        composeTestRule.waitForIdle()
        composeTestRule.waitUntilDefaultTimeout {
            mapFriendlyRouteResponse == actualRailRouteShapes
        }
        assertEquals(mapFriendlyRouteResponse, actualRailRouteShapes)
    }
}
