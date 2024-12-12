package com.mbta.tid.mbta_app.android.nearbyTransit

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class NearbyTransitTabViewModelTest {

    @Test
    fun testSetStopDetailsFilter() {
        val vm = NearbyTransitTabViewModel()
        val newFilter = StopDetailsFilter("route_1", 1)

        assertNull(vm.stopDetailsFilter.value)

        vm.setStopDetailsFilter(newFilter)
        assertEquals(newFilter, vm.stopDetailsFilter.value)
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
