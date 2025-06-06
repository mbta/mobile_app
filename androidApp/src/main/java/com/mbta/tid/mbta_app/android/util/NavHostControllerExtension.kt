package com.mbta.tid.mbta_app.android.util

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath

/**
 * Convert the current nav stack entry into a SheetRoute, then run the provided lambda on it and
 * return the result.
 */
fun <T> NavHostController.fromRoute(take: (SheetRoutes?) -> T): T =
    this.currentBackStackEntry?.let { take(SheetRoutes.fromNavBackStackEntry(it)) } ?: take(null)

val NavHostController.selectedStopId: String?
    get() =
        this.fromRoute {
            when (it) {
                is SheetRoutes.StopDetails -> it.stopId
                else -> null
            }
        }

/** Return true if the current nav entry matches the provided type */
inline fun <reified T : SheetRoutes> NavHostController.routeMatches(): Boolean =
    this.fromRoute { it?.let { it::class == T::class } } == true

/** Return true if the current nav is the route picker and matches the provided path type */
inline fun <reified T : RoutePickerPath> NavHostController.pickerPathMatches(): Boolean =
    this.fromRoute {
        when (it) {
            is SheetRoutes.RoutePicker -> it.path::class == T::class
            else -> false
        }
    }

/** Prevent quick double taps during animation to the new route from retriggering navigation */
inline fun <reified T : SheetRoutes> NavHostController.popBackStackFrom(): Boolean =
    if (this.routeMatches<T>()) this.popBackStack() else false

/** Prevent quick double taps during animation to the new route from retriggering navigation */
inline fun <reified T : SheetRoutes> NavHostController.navigateFrom(
    route: SheetRoutes,
    noinline builder: NavOptionsBuilder.() -> Unit = {},
) = if (this.routeMatches<T>()) this.navigate(route, builder) else {}
