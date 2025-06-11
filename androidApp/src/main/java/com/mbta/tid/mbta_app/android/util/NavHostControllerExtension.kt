package com.mbta.tid.mbta_app.android.util

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import com.mbta.tid.mbta_app.android.SheetRoutes

/** The current stack entry as a SheetRoute, null if none exists */
val NavHostController.currentRoute: SheetRoutes?
    get() = this.currentBackStackEntry?.let { SheetRoutes.fromNavBackStackEntry(it) }

/** If the current nav entry is StopDetails, this is the selected stop ID, otherwise null */
val NavHostController.selectedStopId: String?
    get() = this.currentRouteAs<SheetRoutes.StopDetails>()?.stopId

/** Return the current stack entry as the provided type if they match, otherwise return null */
inline fun <reified T : SheetRoutes> NavHostController.currentRouteAs(): T? =
    this.currentRoute?.let { it as? T }

/** Return true if the current nav entry matches the provided type */
inline fun <reified T : SheetRoutes> NavHostController.currentRouteMatches(): Boolean =
    this.currentRoute?.let { it::class == T::class } == true

/** Prevent quick double taps during animation to the new route from retriggering navigation */
inline fun <reified T : SheetRoutes> NavHostController.navigateFrom(
    route: SheetRoutes,
    noinline builder: NavOptionsBuilder.() -> Unit = {},
) = if (this.currentRouteMatches<T>()) this.navigate(route, builder) else {}

/** Prevent quick double taps during animation to the new route from retriggering navigation */
inline fun <reified T : SheetRoutes> NavHostController.popBackStackFrom(): Boolean =
    if (this.currentRouteMatches<T>()) this.popBackStack() else false
