package com.mbta.tid.mbta_app.android.nearbyTransit

import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class NearbyTransitTabViewModelTest {

    @Test
    fun testSetStopDetailsFilterPushedWhenNotInStopDetails() {
        val vm = NearbyTransitTabViewModel()
        val newFilter = StopDetailsFilter("route_1", 1)

        var popCalled = false
        var pushedRouteCalled = false

        vm.setStopFilter(null, "a", newFilter, { popCalled = true }, { pushedRouteCalled = true })
        assertTrue(pushedRouteCalled)
        assertFalse(popCalled)
    }

    @Test
    fun testSetStopDetailsFilterOldFilterPopped() {
        val vm = NearbyTransitTabViewModel()
        val newFilter = StopDetailsFilter("route_1", 0)

        var popCalled = false
        var pushedRoute: SheetRoutes? = null

        vm.setStopFilter(
            SheetRoutes.StopDetails("a", StopDetailsFilter("route", 1), null),
            "a",
            newFilter,
            { popCalled = true },
            { pushedRoute = it }
        )
        assertEquals(
            newFilter,
            when (val actual = pushedRoute) {
                is SheetRoutes.StopDetails -> actual.stopFilter
                else -> false
            }
        )

        assertTrue(popCalled)
    }

    @Test
    fun testSetStopDetailsFilterDoesNothingIfFilterMatches() {
        val vm = NearbyTransitTabViewModel()
        val sameFilter = StopDetailsFilter("route_1", 0)

        var popCalled = false
        var pushCalled = false

        vm.setStopFilter(
            SheetRoutes.StopDetails("a", sameFilter, null),
            "a",
            sameFilter,
            { popCalled = true },
            { pushCalled = true }
        )
        assertFalse(pushCalled)
        assertFalse(popCalled)
    }

    @Test
    fun testSetStopDetailsFilterOldFilterPoppedWhenNewAutoFilter() {
        val vm = NearbyTransitTabViewModel()
        val newFilter = StopDetailsFilter("route_1", 0, autoFilter = true)

        var popCalled = false
        var pushedRoute: SheetRoutes? = null

        vm.setStopFilter(
            SheetRoutes.StopDetails("a", null, null),
            "a",
            newFilter,
            { popCalled = true },
            { pushedRoute = it }
        )
        assertEquals(
            newFilter,
            when (val actual = pushedRoute) {
                is SheetRoutes.StopDetails -> actual.stopFilter
                else -> false
            }
        )

        assertTrue(popCalled)
    }

    @Test
    fun testSetStopDetailsDepartures() {
        val vm = NearbyTransitTabViewModel()
        val objectCollectionBuilder = ObjectCollectionBuilder()
        val route = objectCollectionBuilder.route {}
        val stop = objectCollectionBuilder.stop {}

        val departures = StopDetailsDepartures(listOf(PatternsByStop(route, stop, emptyList())))

        assertNull(vm.stopDetailsDepartures.value)

        vm.setStopDetailsDepartures(departures)
        assertEquals(departures, vm.stopDetailsDepartures.value)
    }
}
