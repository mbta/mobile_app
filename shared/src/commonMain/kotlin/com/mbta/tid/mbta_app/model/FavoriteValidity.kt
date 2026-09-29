package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse

/**
 * Reasons a saved favorite route/stop/direction combination might not correspond to real,
 * currently-valid schedule data (for example, because of a schedule/rating change or a route/stop
 * being discontinued).
 *
 * Favorites with one of these issues are filtered out of the UI and what's sent to the backend for
 * notification subscriptions, but they are not deleted from local storage.
 */
public enum class FavoriteValidityIssue {
    MissingRoute,
    MissingStop,
    MissingDirection,
    LastStopForRoute,
}

/**
 * Checks whether this route/stop/direction combination corresponds to real, currently-valid
 * schedule data in [global]. Returns the reason it's invalid, or `null` if it's valid.
 */
public fun RouteStopDirection.validityIssue(global: GlobalResponse): FavoriteValidityIssue? =
    checkValidity(this, global)?.let {
        println("Invalid favorite due to $it: $this")
        return@let it
    }

private fun checkValidity(rsd: RouteStopDirection, global: GlobalResponse): FavoriteValidityIssue? {
    val lineOrRoute = global.getLineOrRoute(rsd.route) ?: return FavoriteValidityIssue.MissingRoute
    val stop = global.getStop(rsd.stop) ?: return FavoriteValidityIssue.MissingStop

    val patterns = global.getPatternsFor(rsd.stop, lineOrRoute)

    if (patterns.none { pattern -> pattern.directionId == rsd.direction }) {
        return FavoriteValidityIssue.MissingDirection
    }

    if (stop.isLastStopForAllPatterns(rsd.direction, patterns, global)) {
        return FavoriteValidityIssue.LastStopForRoute
    }

    return null
}

/** Returns the subset of this set which are not valid according to [global], with the reason */
public fun Set<RouteStopDirection>.invalidFavorites(
    global: GlobalResponse
): Map<RouteStopDirection, FavoriteValidityIssue> =
    mapNotNull { rsd -> rsd.validityIssue(global)?.let { rsd to it } }.toMap()

/** Returns a copy of this map without any favorites whose route, stop, or direction is invalid */
public fun Map<RouteStopDirection, FavoriteSettings>.filterValidFavorites(
    global: GlobalResponse
): Map<RouteStopDirection, FavoriteSettings> = filterKeys { it.validityIssue(global) == null }
