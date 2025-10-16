package com.mbta.tid.mbta_app.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class WriteSubscriptionsRequest
internal constructor(
    @SerialName("fcm_token") val fcmToken: String,
    val subscriptions: List<SubscriptionRequest>,
)

@Serializable
public data class UpdateAccessibilityRequest
internal constructor(
    @SerialName("fcm_token") val fcmToken: String,
    @SerialName("include_accessibility") val includeAccessibility: Boolean,
)

@Serializable
public data class WindowRequest
internal constructor(
    @SerialName("start_time") val startTime: LocalTime,
    @SerialName("end_time") val endTime: LocalTime,
    @SerialName("days_of_week") val daysOfWeek: List<Int>,
)

@Serializable
public data class SubscriptionRequest
internal constructor(
    @SerialName("route_id") val routeId: String,
    @SerialName("stop_id") val stopId: String,
    @SerialName("direction_id") val directionId: Int,
    @SerialName("include_accessibility") val includeAccessibility: Boolean,
    val windows: List<WindowRequest>,
) {
    public companion object Companion {
        public fun fromFavorites(
            favorites: Map<RouteStopDirection, FavoriteSettings?>,
            includeAccessibility: Boolean = false,
        ): List<SubscriptionRequest> {
            val enabled = favorites.filter { it.value?.notifications?.enabled == true }
            return enabled.map {
                val rsd = it.key
                val settings = it.value
                val windows =
                    settings?.notifications?.windows?.map {
                        WindowRequest(
                            startTime = it.startTime,
                            endTime = it.endTime,
                            daysOfWeek = it.daysOfWeek.map { day -> day.number },
                        )
                    }
                SubscriptionRequest(
                    routeId = rsd.route.idText,
                    stopId = rsd.stop,
                    directionId = rsd.direction,
                    includeAccessibility = includeAccessibility,
                    windows = windows ?: emptyList(),
                )
            }
        }
    }
}

private val DayOfWeek.number: Int
    get() =
        when (this) {
            DayOfWeek.SUNDAY -> 0
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY -> 2
            DayOfWeek.WEDNESDAY -> 3
            DayOfWeek.THURSDAY -> 4
            DayOfWeek.FRIDAY -> 5
            DayOfWeek.SATURDAY -> 6
        }
