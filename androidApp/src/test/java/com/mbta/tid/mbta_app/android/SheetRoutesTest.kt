package com.mbta.tid.mbta_app.android

import com.mbta.tid.mbta_app.model.SheetRoutes
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import junit.framework.TestCase.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class SheetRouteTest {
    @Test
    fun testPageChangedNearbyToStopDetails() {
        assertTrue(
            SheetRoutes.pageChanged(
                SheetRoutes.NearbyTransit,
                SheetRoutes.StopDetails("a", null, null),
            )
        )
    }

    @Test
    fun testPageChangedWhenStopDetailsDifferentStops() {
        assertTrue(
            SheetRoutes.pageChanged(
                SheetRoutes.StopDetails("a", null, null),
                SheetRoutes.StopDetails("b", null, null),
            )
        )
    }

    @Test
    fun testPageNotChangedWhenStopDetailsSameStopDifferentFilters() {
        assertFalse(
            SheetRoutes.pageChanged(
                SheetRoutes.StopDetails("a", null, null),
                SheetRoutes.StopDetails("a", StopDetailsFilter("route1", 1), null),
            )
        )
    }
}
