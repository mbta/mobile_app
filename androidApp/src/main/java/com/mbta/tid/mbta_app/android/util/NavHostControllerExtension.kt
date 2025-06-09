package com.mbta.tid.mbta_app.android.util

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import com.mbta.tid.mbta_app.android.SheetRoutes

/**
 * Convert the current nav stack entry into a SheetRoute, then run the provided lambda on it and
 * return the result.
 */
fun <T> NavHostController.fromRoute(take: (SheetRoutes?) -> T): T =
    this.currentBackStackEntry?.let { take(SheetRoutes.fromNavBackStackEntry(it)) } ?: take(null)

/** If the current nav entry is StopDetails, this is the selected stop ID, otherwise null */
val NavHostController.selectedStopId: String?
    get() = this.fromRoute { if (it is SheetRoutes.StopDetails) it.stopId else null }

/** Return true if the current nav entry matches the provided type */
inline fun <reified T : SheetRoutes> NavHostController.routeMatches(): Boolean =
    this.fromRoute { it?.let { it::class == T::class } } == true

/** Prevent quick double taps during animation to the new route from retriggering navigation */
inline fun <reified T : SheetRoutes> NavHostController.popBackStackFrom(): Boolean =
    if (this.routeMatches<T>()) this.popBackStack() else false

/** Prevent quick double taps during animation to the new route from retriggering navigation */
inline fun <reified T : SheetRoutes> NavHostController.navigateFrom(
    route: SheetRoutes,
    noinline builder: NavOptionsBuilder.() -> Unit = {},
) = if (this.routeMatches<T>()) this.navigate(route, builder) else {}
