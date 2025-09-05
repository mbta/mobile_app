package com.mbta.tid.mbta_app.android.location

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.plugin.viewport.ViewportStatus
import com.mapbox.maps.plugin.viewport.state.OverviewViewportState
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.android.util.MapAnimationDefaults
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Vehicle
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test

class ViewportProviderTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testFollowUpdatesState() = runBlocking {
        val viewportProvider = ViewportProvider(MapViewportState())
        viewportProvider.isFollowingPuck = false
        viewportProvider.isVehicleOverview = true

        composeTestRule.setContent { LaunchedEffect(Unit) { viewportProvider.follow() } }
        composeTestRule.waitUntilDefaultTimeout { viewportProvider.isFollowingPuck }
        assertTrue(viewportProvider.isFollowingPuck)
        assertFalse(viewportProvider.isVehicleOverview)
    }

    @Test
    fun testLazyPadding() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val stop =
            objects.stop {
                latitude = 42.3531
                longitude = -71.0697
            }
        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                latitude = 42.3547
                longitude = -71.0702
            }
        val mapViewportState =
            MapViewportState().apply {
                setCameraOptions {
                    center(ViewportProvider.Companion.Defaults.center)
                    zoom(ViewportProvider.Companion.Defaults.zoom)
                    pitch(0.0)
                    bearing(0.0)
                    transitionToFollowPuckState()
                }
            }
        val viewportProvider = ViewportProvider(mapViewportState)
        val density = Density(1f)
        val paddingBefore = 0.0
        val paddingAfter = 1.0
        val mapLoaded = CompletableDeferred<Unit>()
        composeTestRule.setContent {
            MapboxMap(mapViewportState = mapViewportState) {
                MapEffect { mapLoaded.complete(Unit) }
            }
        }

        withTimeout(10.seconds) { mapLoaded.await() }

        withTimeout(10.seconds) {
            viewportProvider.setSheetPadding(
                PaddingValues(paddingBefore.dp),
                density,
                LayoutDirection.Ltr,
            )
        }

        val stopCenter =
            async(Dispatchers.Default) {
                delay(1.milliseconds)
                viewportProvider.stopCenter(stop)
            }
        val setPadding =
            async(Dispatchers.Default) {
                delay(2.milliseconds)
                viewportProvider.setSheetPadding(
                    PaddingValues(paddingAfter.dp),
                    density,
                    LayoutDirection.Ltr,
                )
            }
        val vehicleOverview =
            async(Dispatchers.Default) {
                delay(3.milliseconds)
                viewportProvider.vehicleOverview(vehicle, stop, 1f)
            }

        withTimeout(10.seconds) { awaitAll(stopCenter, setPadding, vehicleOverview) }
        try {
            composeTestRule.waitUntilDefaultTimeout {
                mapViewportState.mapViewportStatus is ViewportStatus.State &&
                    (mapViewportState.mapViewportStatus as ViewportStatus.State).state is
                        OverviewViewportState &&
                    ((mapViewportState.mapViewportStatus as ViewportStatus.State).state
                            as OverviewViewportState)
                        .options
                        .padding ==
                        EdgeInsets(paddingAfter, paddingAfter, paddingAfter, paddingAfter)
            }
        } catch (e: ComposeTimeoutException) {
            assertTrue(mapViewportState.mapViewportStatus is ViewportStatus.State)
            assertTrue(
                (mapViewportState.mapViewportStatus as ViewportStatus.State).state
                    is OverviewViewportState
            )
            assertEquals(
                ((mapViewportState.mapViewportStatus as ViewportStatus.State).state
                        as OverviewViewportState)
                    .options
                    .padding,
                EdgeInsets(paddingAfter, paddingAfter, paddingAfter, paddingAfter),
            )
            throw e
        }
    }

    @Test
    fun testLockTimeout() = runBlocking {
        val viewportProvider = ViewportProvider(MapViewportState())
        withTimeout(MapAnimationDefaults.duration * 3) {
            assertNull(viewportProvider.withViewport<Nothing> { suspendCancellableCoroutine {} })
        }
    }
}
