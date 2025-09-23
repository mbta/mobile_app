package com.mbta.tid.mbta_app.utils

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.Favorites
import com.mbta.tid.mbta_app.model.RouteStopDirection
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

public class FavoritesBuilder {
    private val routeStopDirection = mutableMapOf<RouteStopDirection, FavoriteSettings>()

    @DefaultArgumentInterop.Enabled
    public fun routeStopDirection(
        route: String,
        stop: String,
        direction: Int,
        settings: SettingsBuilder.() -> Unit = {},
    ) {
        this.routeStopDirection(RouteStopDirection(route, stop, direction), settings)
    }

    @DefaultArgumentInterop.Enabled
    public fun routeStopDirection(
        rsd: RouteStopDirection,
        settings: SettingsBuilder.() -> Unit = {},
    ) {
        this.routeStopDirection.put(rsd, SettingsBuilder().apply(settings).build())
    }

    internal fun build() = Favorites(routeStopDirection)

    public class SettingsBuilder {
        private var notifications = FavoriteSettings.Notifications.disabled

        public fun notifications(block: NotificationsBuilder.() -> Unit) {
            notifications = NotificationsBuilder().apply(block).build()
        }

        internal fun build() = FavoriteSettings(notifications)

        public class NotificationsBuilder {
            public var enabled: Boolean = false
            public var windows: Set<FavoriteSettings.Notifications.Window> = emptySet()

            public fun window(
                startTime: LocalTime,
                endTime: LocalTime,
                daysOfWeek: Set<DayOfWeek>,
            ) {
                windows =
                    windows + FavoriteSettings.Notifications.Window(startTime, endTime, daysOfWeek)
            }

            internal fun build() = FavoriteSettings.Notifications(enabled, windows)
        }
    }
}

public fun buildFavorites(block: FavoritesBuilder.() -> Unit): Favorites =
    FavoritesBuilder().apply(block).build()
