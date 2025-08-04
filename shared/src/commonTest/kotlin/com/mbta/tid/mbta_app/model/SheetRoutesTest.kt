package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun testShouldResetSheetHeightStopDetails() {
        assertTrue(
            SheetRoutes.shouldResetSheetHeight(
                SheetRoutes.StopDetails("a", null, null),
                SheetRoutes.StopDetails("b", null, null),
            )
        )

        assertFalse(
            SheetRoutes.shouldResetSheetHeight(
                SheetRoutes.StopDetails("a", StopDetailsFilter("b", 0), null),
                SheetRoutes.StopDetails("a", StopDetailsFilter("b", 1), null),
            )
        )

        assertTrue(
            SheetRoutes.shouldResetSheetHeight(
                SheetRoutes.NearbyTransit,
                SheetRoutes.StopDetails("a", StopDetailsFilter("b", 1), null),
            )
        )

        assertTrue(
            SheetRoutes.shouldResetSheetHeight(
                SheetRoutes.Favorites,
                SheetRoutes.StopDetails("a", StopDetailsFilter("b", 1), null),
            )
        )
    }

    @Test
    fun testShouldRestSheetHeightAddOrEditFavorites() {
        assertFalse(
            SheetRoutes.shouldResetSheetHeight(SheetRoutes.Favorites, SheetRoutes.EditFavorites)
        )
        assertFalse(
            SheetRoutes.shouldResetSheetHeight(
                SheetRoutes.Favorites,
                SheetRoutes.RoutePicker(RoutePickerPath.Bus, RouteDetailsContext.Favorites),
            )
        )
        assertFalse(
            SheetRoutes.shouldResetSheetHeight(
                SheetRoutes.RoutePicker(RoutePickerPath.Bus, RouteDetailsContext.Favorites),
                SheetRoutes.RouteDetails("a", RouteDetailsContext.Favorites),
            )
        )
        assertFalse(
            SheetRoutes.shouldResetSheetHeight(
                SheetRoutes.RouteDetails("a", RouteDetailsContext.Favorites),
                SheetRoutes.Favorites,
            )
        )
    }
}
