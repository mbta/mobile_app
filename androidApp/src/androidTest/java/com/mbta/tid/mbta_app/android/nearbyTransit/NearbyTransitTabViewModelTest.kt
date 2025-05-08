package com.mbta.tid.mbta_app.android.nearbyTransit

import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.Clock
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
    fun testSetRouteCardData() {
        val vm = NearbyTransitTabViewModel()
        val objectCollectionBuilder = ObjectCollectionBuilder()
        val route = objectCollectionBuilder.route {}
        val stop = objectCollectionBuilder.stop {}

        val routeCardData =
            listOf(
                RouteCardData(
                    RouteCardData.LineOrRoute.Route(route),
                    listOf(
                        RouteCardData.RouteStopData(
                            stop,
                            route,
                            listOf(),
                            GlobalResponse(objectCollectionBuilder)
                        )
                    ),
                    RouteCardData.Context.NearbyTransit,
                    Clock.System.now()
                )
            )

        assertNull(vm.routeCardData.value)

        vm.setRouteCardData(routeCardData)
        assertEquals(routeCardData, vm.routeCardData.value)
    }
}
