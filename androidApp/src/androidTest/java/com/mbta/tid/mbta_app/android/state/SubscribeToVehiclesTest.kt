package com.mbta.tid.mbta_app.android.state

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import com.mbta.tid.mbta_app.repositories.MockVehiclesRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SubscribeToVehiclesTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Test structure based on lifecycle tests:
    // https://github.com/androidx/androidx/blob/0b709f31b71110fe671ed8b4c03f96ad30a0cd37/lifecycle/lifecycle-runtime-compose/src/androidInstrumentedTest/kotlin/androidx/lifecycle/compose/LifecycleEffectTest.kt#L247
    @Test
    fun testSubscribeToVehicles() = runComposeUiTest {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        val vehicle =
            ObjectCollectionBuilder().vehicle { currentStatus = Vehicle.CurrentStatus.StoppedAt }
        var connectProps: Pair<String, Int>? = null

        val vehiclesRepo =
            MockVehiclesRepository(
                VehiclesStreamDataResponse(mapOf(vehicle.id to vehicle)),
                onConnect = { routeId, directionId -> connectProps = Pair(routeId, directionId) }
            )

        var vehicles: List<Vehicle> = emptyList()

        var stateFilter = mutableStateOf(StopDetailsFilter("route_1", 1))

        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                var filter by remember { stateFilter }
                vehicles = subscribeToVehicles(filter, vehiclesRepo)
            }
        }

        Log.i("KB", "await idle")
        waitUntil { connectProps == Pair("route_1", 1) }
        assertEquals(listOf(vehicle), vehicles)

        runOnUiThread {
            stateFilter.value = StopDetailsFilter("route_2", 1)
            Log.i("KB", "filter set")
        }
        waitUntil { connectProps == Pair("route_2", 1) }
    }

    @Test
    fun testDisconnectsOnPause() = runComposeUiTest {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        val vehicle =
            ObjectCollectionBuilder().vehicle { currentStatus = Vehicle.CurrentStatus.StoppedAt }
        var connectCount = 0
        var disconnectCount = 0

        val vehiclesRepo =
            MockVehiclesRepository(
                VehiclesStreamDataResponse(mapOf(vehicle.id to vehicle)),
                onConnect = { _, _ -> connectCount += 1 },
                onDisconnect = { disconnectCount += 1 }
            )

        var vehicles: List<Vehicle> = emptyList()

        var stateFilter = mutableStateOf(StopDetailsFilter("route_1", 1))

        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                var filter by remember { stateFilter }
                vehicles = subscribeToVehicles(filter, vehiclesRepo)
            }
        }

        Log.i("KB", "await idle")
        waitUntil { connectCount == 1 }
        // Disconnect called before connecting
        assertEquals(1, disconnectCount)

        runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE) }

        waitUntil { disconnectCount == 2 }
        assertEquals(2, disconnectCount)
        assertEquals(1, connectCount)

        runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }

        waitUntil { disconnectCount == 3 }
        assertEquals(2, connectCount)
    }
}
