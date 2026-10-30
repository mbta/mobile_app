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
public fun RouteStopDirection.validityIssue(global: GlobalResponse): FavoriteValidityIssue? {
    val lineOrRoute = global.getLineOrRoute(this.route) ?: return FavoriteValidityIssue.MissingRoute
    val stop = global.getStop(this.stop) ?: return FavoriteValidityIssue.MissingStop

    val patterns = global.getPatternsFor(this.stop, lineOrRoute)

    if (patterns.none { pattern -> pattern.directionId == this.direction }) {
        return FavoriteValidityIssue.MissingDirection
    }

    if (stop.isLastStopForAllPatterns(this.direction, patterns, global)) {
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
