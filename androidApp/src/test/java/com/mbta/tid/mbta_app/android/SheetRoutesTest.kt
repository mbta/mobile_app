package com.mbta.tid.mbta_app.android

import com.mbta.tid.mbta_app.model.SheetRoutes
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
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

    @Test
    fun testRetainSheetSizeWhenNotStopDetailsOrEntrypoint() {
        assertFalse(
            SheetRoutes.retainSheetSize(
                SheetRoutes.StopDetails("a", null, null),
                SheetRoutes.NearbyTransit,
            )
        )

        assertFalse(SheetRoutes.retainSheetSize(SheetRoutes.NearbyTransit, SheetRoutes.Favorites))

        assertTrue(
            SheetRoutes.retainSheetSize(
                SheetRoutes.RoutePicker(RoutePickerPath.Bus, RouteDetailsContext.Favorites),
                SheetRoutes.Favorites,
            )
        )

        assertTrue(SheetRoutes.retainSheetSize(SheetRoutes.EditFavorites, SheetRoutes.Favorites))
    }
}
