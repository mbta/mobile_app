package com.mbta.tid.mbta_app.model.response

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.routes.DeepLinkState
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class PushNotificationPayload(
    val title: Title,
    val summary: AlertSummary,
    val alertId: String,
    val subscriptions: List<RouteStopDirection>,
    val notificationType: NotificationType,
    val sentAt: Instant,
) {
    @Serializable
    public sealed class Title {
        @Serializable
        @SerialName("bare_label")
        public data class BareLabel(val label: String) : Title()

        @Serializable
        @SerialName("mode_label")
        public data class ModeLabel(val label: String, val mode: RouteType) : Title()

        @Serializable @SerialName("multiple_routes") public data object MultipleRoutes : Title()

        @Serializable public data class Unknown(val fallback: Title) : Title()
    }

    @Serializable
    public enum class NotificationType {
        @SerialName("notification") Notification,
        @SerialName("reminder") Reminder,
        @SerialName("all_clear") AllClear,
    }

    public enum class StillActive {
        Yes,
        No,
        Unknown,
        Reminder,
        AllClear,
    }

    public fun getDeepLinkState(): DeepLinkState {
        val stopId = subscriptions.distinctBy { it.stop }.singleOrNull()?.stop
        val routeId = subscriptions.distinctBy { it.route }.singleOrNull()?.route?.idText
        val directionId = subscriptions.distinctBy { it.direction }.singleOrNull()?.direction
        return if (stopId != null) {
            DeepLinkState.Stop(stopId, routeId, directionId, tripId = null)
        } else {
            DeepLinkState.Alert(alertId, routeId, stopId)
        }
    }

    @DefaultArgumentInterop.Enabled
    public fun isStillActive(now: EasternTimeInstant = EasternTimeInstant.now()): StillActive {
        val sentAt by lazy { EasternTimeInstant(sentAt) }
        if (summary.update == AlertSummary.Update.AllClear) {
            return StillActive.AllClear
        }
        val recurrenceEndTime =
            when (summary.recurrence) {
                is AlertSummary.Recurrence.Daily -> summary.recurrence.ending
                is AlertSummary.Recurrence.SomeDays -> summary.recurrence.ending
                AlertSummary.Recurrence.Unknown,
                null -> null
            }
        when (recurrenceEndTime) {
            AlertSummary.Timeframe.UntilFurtherNotice,
            AlertSummary.Timeframe.Unknown,
            null -> {
                // unknown from recurrence, but check timeframe
            }
            AlertSummary.Timeframe.Tomorrow -> {
                val tomorrow = sentAt.serviceDate.plus(DatePeriod(days = 1))
                val tomorrowStart = EasternTimeInstant(tomorrow, LocalTime(3, 0))
                val tomorrowEnd =
                    EasternTimeInstant(tomorrow.plus(DatePeriod(days = 1)), LocalTime(3, 0))
                if (now < tomorrowStart) return StillActive.Yes
                if (now > tomorrowEnd) return StillActive.No
                // unknown from recurrence, but check timeframe
            }
            is AlertSummary.Timeframe.ThisWeek -> {
                return if (now < recurrenceEndTime.time) StillActive.Yes else StillActive.No
            }
            is AlertSummary.Timeframe.LaterDate -> {
                return if (now < recurrenceEndTime.time) StillActive.Yes else StillActive.No
            }
        }
        // if recurrence existed but was inconclusive, timeframe ending means Unknown, not No,
        // so we fall through when the timeframe has ended
        when (summary.timeframe) {
            AlertSummary.Timeframe.UntilFurtherNotice,
            AlertSummary.Timeframe.Unknown,
            null -> return StillActive.Unknown
            is AlertSummary.Timeframe.Time ->
                if (now < summary.timeframe.time) return StillActive.Yes
            AlertSummary.Timeframe.EndOfService -> {
                val endTime =
                    EasternTimeInstant(
                        sentAt.serviceDate.plus(DatePeriod(days = 1)),
                        LocalTime(3, 0),
                    )
                if (now < endTime) return StillActive.Yes
            }
            AlertSummary.Timeframe.Tomorrow -> {
                val tomorrow = sentAt.serviceDate.plus(DatePeriod(days = 1))
                val tomorrowStart = EasternTimeInstant(tomorrow, LocalTime(3, 0))
                val tomorrowEnd =
                    EasternTimeInstant(tomorrow.plus(DatePeriod(days = 1)), LocalTime(3, 0))
                if (now < tomorrowStart) return StillActive.Yes
                if (now < tomorrowEnd) return StillActive.Unknown
            }
            is AlertSummary.Timeframe.ThisWeek ->
                if (now < summary.timeframe.time) return StillActive.Yes
            is AlertSummary.Timeframe.LaterDate ->
                if (now < summary.timeframe.time) return StillActive.Yes
            AlertSummary.Timeframe.StartingTomorrow,
            is AlertSummary.Timeframe.StartingLaterToday -> return StillActive.Reminder
            is AlertSummary.Timeframe.TimeRange -> {
                val today = sentAt.serviceDate
                val startTime =
                    when (summary.timeframe.startTime) {
                        AlertSummary.Timeframe.TimeRange.StartOfService ->
                            EasternTimeInstant(today, LocalTime(3, 0))
                        is AlertSummary.Timeframe.TimeRange.Time -> summary.timeframe.startTime.time
                        AlertSummary.Timeframe.TimeRange.Unknown -> return StillActive.Unknown
                    }
                val endTime =
                    when (summary.timeframe.endTime) {
                        is AlertSummary.Timeframe.TimeRange.Time -> summary.timeframe.endTime.time
                        AlertSummary.Timeframe.TimeRange.EndOfService ->
                            EasternTimeInstant(today.plus(DatePeriod(days = 1)), LocalTime(3, 0))
                        AlertSummary.Timeframe.TimeRange.Unknown -> return StillActive.Unknown
                    }
                if (now >= startTime && now < endTime) return StillActive.Yes
            }
        }
        return if (summary.recurrence != null) {
            // recurrence existed but didn’t determine result, current timeframe ended but next one
            // may or may not exist and have started
            StillActive.Unknown
        } else {
            StillActive.No
        }
    }

    public companion object {
        public val launchKey: String =
            "com.mbta.tid.mbta_app.model.response.PushNotificationPayload"

        public fun serialize(payload: PushNotificationPayload): String =
            json.encodeToString(payload)

        public fun deserialize(rawPayload: String): PushNotificationPayload =
            json.decodeFromString(rawPayload)
    }
}
