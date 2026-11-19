package com.mbta.tid.mbta_app.routes

import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteStopFilter
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SheetRouteTest {
    @Test
    fun testPageChangedNearbyToStopOverview() {
        assertTrue(
            SheetRoutes.pageChanged(
                SheetRoutes.NearbyTransit,
                SheetRoutes.StopOverview("a"),
            ),
        )
    }

    @Test
    fun testPageChangedWhenStopOverviewDifferentStops() {
        assertTrue(
            SheetRoutes.pageChanged(
                SheetRoutes.StopOverview("a"),
                SheetRoutes.StopOverview("b"),
            ),
        )
    }

    @Test
    fun testPageNotChangedWhenStopOverviewToRouteStop() {
        assertFalse(
            SheetRoutes.pageChanged(
                SheetRoutes.StopOverview("a"),
                SheetRoutes.RouteStop("a", RouteStopFilter(Route.Id("route1"), 1), null),
            ),
        )
    }

    @Test
    fun testRetainSheetSizeWhenNotStopOrEntrypoint() {
        assertFalse(
            SheetRoutes.retainSheetSize(
                SheetRoutes.StopOverview("a"),
                SheetRoutes.NearbyTransit,
            ),
        )

        assertFalse(SheetRoutes.retainSheetSize(SheetRoutes.NearbyTransit, SheetRoutes.Favorites))

        assertTrue(
            SheetRoutes.retainSheetSize(
                SheetRoutes.RoutePicker(RoutePickerPath.Bus, RouteDetailsContext.Favorites),
                SheetRoutes.Favorites,
            ),
        )

        assertTrue(SheetRoutes.retainSheetSize(SheetRoutes.EditFavorites, SheetRoutes.Favorites))
    }

    @Test
    fun testShouldResetSheetHeightStopCentric() {
        assertTrue(
            SheetRoutes.shouldResetSheetHeight(
                SheetRoutes.StopOverview("a"),
                SheetRoutes.StopOverview("b"),
            ),
        )

        assertFalse(
            SheetRoutes.shouldResetSheetHeight(
                SheetRoutes.RouteStop("a", RouteStopFilter(Route.Id("b"), 0), null),
                SheetRoutes.RouteStop("a", RouteStopFilter(Route.Id("b"), 1), null),
            ),
        )

        assertTrue(
            SheetRoutes.shouldResetSheetHeight(
                SheetRoutes.NearbyTransit,
                SheetRoutes.RouteStop("a", RouteStopFilter(Route.Id("b"), 1), null),
            ),
        )

        assertTrue(
            SheetRoutes.shouldResetSheetHeight(
                SheetRoutes.Favorites,
                SheetRoutes.RouteStop("a", RouteStopFilter(Route.Id("b"), 1), null),
            ),
        )
    }

    @Test
    fun testShouldRestSheetHeightAddOrEditFavorites() {
        assertFalse(
            SheetRoutes.shouldResetSheetHeight(SheetRoutes.Favorites, SheetRoutes.EditFavorites),
        )
        assertFalse(
            SheetRoutes.shouldResetSheetHeight(
                SheetRoutes.Favorites,
                SheetRoutes.RoutePicker(RoutePickerPath.Bus, RouteDetailsContext.Favorites),
            ),
        )
        assertFalse(
            SheetRoutes.shouldResetSheetHeight(
                SheetRoutes.RoutePicker(RoutePickerPath.Bus, RouteDetailsContext.Favorites),
                SheetRoutes.RouteDetails(Route.Id("a"), RouteDetailsContext.Favorites),
            ),
        )
        assertFalse(
            SheetRoutes.shouldResetSheetHeight(
                SheetRoutes.RouteDetails(Route.Id("a"), RouteDetailsContext.Favorites),
                SheetRoutes.Favorites,
            ),
        )
    }
}
