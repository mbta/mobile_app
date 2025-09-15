package com.mbta.tid.mbta_app.android.state

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import com.mbta.tid.mbta_app.repositories.MockVehiclesRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.MockRouteCardDataViewModel
import com.mbta.tid.mbta_app.viewModel.RouteCardDataViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SubscribeToVehiclesTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Test structure based on lifecycle tests:
    // https://github.com/androidx/androidx/blob/0b709f31b71110fe671ed8b4c03f96ad30a0cd37/lifecycle/lifecycle-runtime-compose/src/androidInstrumentedTest/kotlin/androidx/lifecycle/compose/LifecycleEffectTest.kt#L247
    @Test
    fun testSubscribeToVehicles() = runTest {
        val vehicle =
            ObjectCollectionBuilder().vehicle { currentStatus = Vehicle.CurrentStatus.StoppedAt }
        var connectProps: Pair<String, Int>? = null

        val vehiclesRepo =
            MockVehiclesRepository(
                VehiclesStreamDataResponse(mapOf(vehicle.id to vehicle)),
                onConnect = { routeId, directionId -> connectProps = Pair(routeId, directionId) },
            )

        var vehicles: List<Vehicle> = emptyList()

        var stateFilter = mutableStateOf(StopDetailsFilter("route_1", 1))

        composeTestRule.setContent {
            var filter by remember { stateFilter }
            vehicles = subscribeToVehicles(filter, MockRouteCardDataViewModel(), vehiclesRepo)
        }

        composeTestRule.waitUntilDefaultTimeout { connectProps == Pair("route_1", 1) }
        composeTestRule.waitUntilDefaultTimeout { listOf(vehicle) == vehicles }

        composeTestRule.runOnUiThread { stateFilter.value = StopDetailsFilter("route_2", 1) }
        composeTestRule.waitUntilDefaultTimeout { connectProps == Pair("route_2", 1) }
    }

    @Test
    fun testDisconnectsOnPause() = runTest {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        val vehicle =
            ObjectCollectionBuilder().vehicle { currentStatus = Vehicle.CurrentStatus.StoppedAt }
        var connectCount = 0
        var disconnectCount = 0

        val vehiclesRepo =
            MockVehiclesRepository(
                VehiclesStreamDataResponse(mapOf(vehicle.id to vehicle)),
                onConnect = { _, _ -> connectCount += 1 },
                onDisconnect = { disconnectCount += 1 },
            )

        var vehicles: List<Vehicle> = emptyList()

        var stateFilter = mutableStateOf(StopDetailsFilter("route_1", 1))

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                var filter by remember { stateFilter }
                vehicles = subscribeToVehicles(filter, MockRouteCardDataViewModel(), vehiclesRepo)
            }
        }

        composeTestRule.waitUntilDefaultTimeout { connectCount == 1 }
        // Disconnect called before connecting
        assertEquals(1, disconnectCount)

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE) }

        composeTestRule.waitUntilDefaultTimeout { disconnectCount == 2 }
        assertEquals(2, disconnectCount)
        assertEquals(1, connectCount)

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }

        composeTestRule.waitUntilDefaultTimeout { disconnectCount == 3 }
        composeTestRule.waitUntilDefaultTimeout { connectCount == 2 }
        assertEquals(2, connectCount)
    }

    @Test
    fun testFiltersIrrelevantVehicles() = runTest {
        val objects = ObjectCollectionBuilder()

        val route1 = objects.route { id = "A" }
        val route2 = objects.route { id = "B" }

        val vehicle1 =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                routeId = route1.id
            }
        val vehicle2 =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                routeId = route2.id
            }

        var connectProps: Pair<String, Int>? = null

        val vehiclesRepo =
            MockVehiclesRepository(
                VehiclesStreamDataResponse(mapOf(vehicle1.id to vehicle1, vehicle2.id to vehicle2)),
                onConnect = { routeId, directionId -> connectProps = Pair(routeId, directionId) },
            )

        val line = RouteCardData.LineOrRoute.Line(objects.line(), routes = setOf(route1, route2))
        val stop = objects.stop()
        val routeCardData =
            listOf(
                RouteCardData(
                    lineOrRoute = line,
                    stopData =
                        listOf(
                            RouteCardData.RouteStopData(
                                line,
                                stop,
                                directions = listOf(),
                                listOf(
                                    RouteCardData.Leaf(
                                        line,
                                        stop,
                                        0,
                                        listOf(),
                                        setOf(stop.id),
                                        listOf(UpcomingTrip(objects.trip { routeId = route2.id })),
                                        emptyList(),
                                        true,
                                        true,
                                        emptyList(),
                                        RouteCardData.Context.StopDetailsFiltered,
                                    )
                                ),
                            )
                        ),
                    at = EasternTimeInstant.now(),
                )
            )

        var vehicles: List<Vehicle> = emptyList()

        val stateFilter = mutableStateOf(StopDetailsFilter(line.id, 0))

        val routeCardDataVM =
            MockRouteCardDataViewModel(RouteCardDataViewModel.State(routeCardData))

        composeTestRule.setContent {
            val filter by remember { stateFilter }
            vehicles = subscribeToVehicles(filter, routeCardDataVM, vehiclesRepo)
        }

        composeTestRule.waitUntilDefaultTimeout { connectProps == Pair(line.id, 0) }
        composeTestRule.waitUntilDefaultTimeout { listOf(vehicle2) == vehicles }
    }
}
