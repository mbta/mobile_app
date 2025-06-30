package com.mbta.tid.mbta_app.android.util

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import com.mbta.tid.mbta_app.android.fromNavBackStackEntry
import com.mbta.tid.mbta_app.model.SheetRoutes
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/** The current stack entry as a SheetRoute, null if none exists */
val NavHostController.currentRoute: SheetRoutes?
    get() = this.currentBackStackEntry?.let { SheetRoutes.fromNavBackStackEntry(it) }

/** If the current nav entry is StopDetails, this is the selected stop ID, otherwise null */
val NavHostController.selectedStopId: String?
    get() = this.currentRouteAs(SheetRoutes.StopDetails::class)?.stopId

/** Return the current stack entry as the provided type if they match, otherwise return null */
fun <T : SheetRoutes> NavHostController.currentRouteAs(routeType: KClass<T>): T? =
    routeType.safeCast(this.currentRoute)

/** Return true if the current nav entry matches the provided type */
fun <T : SheetRoutes> NavHostController.currentRouteMatches(routeType: KClass<T>): Boolean =
    this.currentRoute?.let { it::class == routeType } == true

/** Prevent quick double taps during animation to the new route from retriggering navigation */
fun <T : SheetRoutes> NavHostController.navigateFrom(
    routeType: KClass<T>,
    route: SheetRoutes,
    builder: NavOptionsBuilder.() -> Unit = {},
) = if (this.currentRouteMatches(routeType)) this.navigate(route, builder) else {}

/** Prevent quick double taps during animation to the new route from retriggering navigation */
fun <T : SheetRoutes> NavHostController.popBackStackFrom(routeType: KClass<T>): Boolean =
    if (this.currentRouteMatches(routeType)) this.popBackStack() else false
