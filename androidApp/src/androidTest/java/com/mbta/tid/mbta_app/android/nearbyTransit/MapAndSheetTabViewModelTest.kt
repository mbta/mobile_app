package com.mbta.tid.mbta_app.android.nearbyTransit

import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.routes.SheetRoutes
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlin.test.assertEquals
import org.junit.Test

class MapAndSheetTabViewModelTest {

    @Test
    fun testUpdatesStopDetailsPageFilterUpdates() {
        val vm = NearbyTransitTabViewModel()
        val initialFilter =
            StopDetailsPageFilters(
                "stop1",
                StopDetailsFilter(Route.Id("route_1"), 1),
                TripDetailsFilter("trip_1", null, 0, false),
            )
        val initialNav =
            SheetRoutes.StopDetails(
                initialFilter.stopId,
                initialFilter.stopFilter,
                initialFilter.tripFilter,
            )
        val newFilter =
            StopDetailsPageFilters(
                "stop1",
                StopDetailsFilter(Route.Id("route_1"), 0),
                TripDetailsFilter("trip_2", null, 0, false),
            )
        val newNav =
            SheetRoutes.StopDetails(newFilter.stopId, newFilter.stopFilter, newFilter.tripFilter)

        var popCalled = false
        var pushedRoute: SheetRoutes? = null

        vm.setStopDetailsFilters(null, initialFilter, { popCalled = true }, { pushedRoute = it })
        assertEquals(initialNav, pushedRoute)
        assertFalse(popCalled)
        vm.setStopDetailsFilters(pushedRoute, newFilter, { popCalled = true }, { pushedRoute = it })
        assertEquals(newNav, pushedRoute)
        assertTrue(popCalled)
    }

    @Test
    fun testSetStopDetailsFilterPushedWhenNotInStopDetails() {
        val vm = NearbyTransitTabViewModel()
        val newFilter = StopDetailsFilter(Route.Id("route_1"), 1)

        var popCalled = false
        var pushedRouteCalled = false

        vm.setStopFilter(null, "a", newFilter, { popCalled = true }, { pushedRouteCalled = true })
        assertTrue(pushedRouteCalled)
        assertFalse(popCalled)
    }

    @Test
    fun testSetStopDetailsFilterOldFilterPopped() {
        val vm = NearbyTransitTabViewModel()
        val newFilter = StopDetailsFilter(Route.Id("route_1"), 0)

        var popCalled = false
        var pushedRoute: SheetRoutes? = null

        vm.setStopFilter(
            SheetRoutes.StopDetails("a", StopDetailsFilter(Route.Id("route"), 1), null),
            "a",
            newFilter,
            { popCalled = true },
            { pushedRoute = it },
        )
        assertEquals(
            newFilter,
            when (val actual = pushedRoute) {
                is SheetRoutes.StopDetails -> actual.stopFilter
                else -> false
            },
        )

        assertTrue(popCalled)
    }

    @Test
    fun testSetStopDetailsFilterDoesNothingIfFilterMatches() {
        val vm = NearbyTransitTabViewModel()
        val sameFilter = StopDetailsFilter(Route.Id("route_1"), 0)

        var popCalled = false
        var pushCalled = false

        vm.setStopFilter(
            SheetRoutes.StopDetails("a", sameFilter, null),
            "a",
            sameFilter,
            { popCalled = true },
            { pushCalled = true },
        )
        assertFalse(pushCalled)
        assertFalse(popCalled)
    }

    @Test
    fun testSetStopDetailsFilterOldFilterPoppedWhenNewAutoFilter() {
        val vm = NearbyTransitTabViewModel()
        val newFilter = StopDetailsFilter(Route.Id("route_1"), 0, autoFilter = true)

        var popCalled = false
        var pushedRoute: SheetRoutes? = null

        vm.setStopFilter(
            SheetRoutes.StopDetails("a", null, null),
            "a",
            newFilter,
            { popCalled = true },
            { pushedRoute = it },
        )
        assertEquals(
            newFilter,
            when (val actual = pushedRoute) {
                is SheetRoutes.StopDetails -> actual.stopFilter
                else -> false
            },
        )

        assertTrue(popCalled)
    }
}
