package com.mbta.tid.mbta_app.analytics

import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsTest {

    @Test
    fun testLogsUpdateFavorite() {
        val favoriteAdded = RouteStopDirection("route_1", "stop_1", 0)

        var eventLogged: Pair<String, Map<String, String>>? = null

        val analytics =
            MockAnalytics(onLogEvent = { event, params -> eventLogged = Pair(event, params) })

        analytics.favoritesUpdated(mapOf(favoriteAdded to true), EditFavoritesContext.Favorites, 0)

        assertEquals(
            eventLogged,
            Pair(
                "updated_favorites",
                mapOf(
                    "action" to "add",
                    "route_id" to favoriteAdded.route,
                    "stop_id" to favoriteAdded.stop,
                    "direction_id" to "${favoriteAdded.direction}",
                    "is_default_direction" to "true",
                    "context" to "Favorites",
                    "updated_both_directions_at_once" to "false",
                ),
            ),
        )
    }

    @Test
    fun testLogsUpdateMultipleFavoritesAtOnce() {
        val favoriteAdded = RouteStopDirection("route_1", "stop_1", 0)
        val favoriteRemoved = RouteStopDirection("route_1", "stop_1", 1)

        var eventsLogged = mutableListOf<Pair<String, Map<String, String>>>()

        val analytics =
            MockAnalytics(onLogEvent = { event, params -> eventsLogged.add(Pair(event, params)) })

        analytics.favoritesUpdated(
            mapOf(favoriteAdded to true, favoriteRemoved to false),
            EditFavoritesContext.Favorites,
            0,
        )

        assertEquals(
            eventsLogged,
            listOf(
                Pair(
                    "updated_favorites",
                    mapOf(
                        "action" to "add",
                        "route_id" to favoriteAdded.route,
                        "stop_id" to favoriteAdded.stop,
                        "direction_id" to "${favoriteAdded.direction}",
                        "is_default_direction" to "true",
                        "context" to "Favorites",
                        "updated_both_directions_at_once" to "true",
                    ),
                ),
                Pair(
                    "updated_favorites",
                    mapOf(
                        "action" to "remove",
                        "route_id" to favoriteRemoved.route,
                        "stop_id" to favoriteRemoved.stop,
                        "direction_id" to "${favoriteRemoved.direction}",
                        "is_default_direction" to "false",
                        "context" to "Favorites",
                        "updated_both_directions_at_once" to "true",
                    ),
                ),
            ),
        )
    }

    @Test
    fun testTrack() {
        var loggedEvent: Pair<String, Map<String, String>>? = null
        val analytics = MockAnalytics({ event, params -> loggedEvent = Pair(event, params) })
        analytics.track(AnalyticsScreen.Favorites)
        assertEquals(
            loggedEvent,
            Pair(
                Analytics.ANALYTICS_EVENT_SCREEN_VIEW,
                mapOf(Analytics.ANALYTICS_PARAMETER_SCREEN_NAME to "FavoritesPage"),
            ),
        )
        assertEquals(analytics.lastTrackedScreen, AnalyticsScreen.Favorites)
    }

    @Test
    fun testTappedDeparture() {
        var loggedEvent: Pair<String, Map<String, String>>? = null
        val analytics = MockAnalytics({ event, params -> loggedEvent = Pair(event, params) })
        analytics.track(AnalyticsScreen.Favorites)
        analytics.tappedDeparture("route_1", "stop_1", false, false, RouteType.BUS, null)
        assertEquals(
            loggedEvent,
            Pair(
                "tapped_departure",
                mapOf(
                    "route_id" to "route_1",
                    "stop_id" to "stop_1",
                    "pinned" to "false",
                    "alert" to "false",
                    "mode" to "bus",
                    "no_trips" to "",
                    "context" to "FavoritesPage",
                ),
            ),
        )
    }
}
