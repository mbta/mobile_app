package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.routes.DeepLinkState
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Month

class PushNotificationPayloadTest {
    @Test
    fun `getDeepLinkState preserves route stop direction`() {
        val route = Route.Id("route")
        val stop = "stop"
        val direction = 0
        val payload =
            PushNotificationPayload(
                AlertSummary(Alert.Effect.Suspension),
                "alert",
                listOf(RouteStopDirection(route, stop, direction)),
                PushNotificationPayload.NotificationType.Notification,
                Clock.System.now(),
            )
        assertEquals(
            DeepLinkState.Stop(stop, route.idText, direction, tripId = null),
            payload.getDeepLinkState(),
        )
    }

    @Test
    fun `getDeepLinkState preserves route stop`() {
        val route = Route.Id("route")
        val stop = "stop"
        val payload =
            PushNotificationPayload(
                AlertSummary(Alert.Effect.Suspension),
                "alert",
                listOf(RouteStopDirection(route, stop, 0), RouteStopDirection(route, stop, 1)),
                PushNotificationPayload.NotificationType.Notification,
                Clock.System.now(),
            )
        assertEquals(
            DeepLinkState.Stop(stop, route.idText, directionId = null, tripId = null),
            payload.getDeepLinkState(),
        )
    }

    @Test
    fun `getDeepLinkState preserves stop`() {
        val stop = "stop"
        val payload =
            PushNotificationPayload(
                AlertSummary(Alert.Effect.Suspension),
                "alert",
                listOf(
                    RouteStopDirection(Route.Id("route1"), stop, 0),
                    RouteStopDirection(Route.Id("route2"), stop, 1),
                ),
                PushNotificationPayload.NotificationType.Notification,
                Clock.System.now(),
            )
        assertEquals(
            DeepLinkState.Stop(stop, routeId = null, directionId = null, tripId = null),
            payload.getDeepLinkState(),
        )
    }

    @Test
    fun `getDeepLinkState falls back to alert with route`() {
        val route = Route.Id("route")
        val alert = "alert"
        val payload =
            PushNotificationPayload(
                AlertSummary(Alert.Effect.Suspension),
                "alert",
                listOf(
                    RouteStopDirection(route, "stop1", 0),
                    RouteStopDirection(route, "stop2", 1),
                ),
                PushNotificationPayload.NotificationType.Notification,
                Clock.System.now(),
            )
        assertEquals(
            DeepLinkState.Alert(alert, route.idText, stopId = null),
            payload.getDeepLinkState(),
        )
    }

    @Test
    fun `getDeepLinkState falls back to alert without route`() {
        val alert = "alert"
        val payload =
            PushNotificationPayload(
                AlertSummary(Alert.Effect.Suspension),
                "alert",
                listOf(
                    RouteStopDirection(Route.Id("route1"), "stop1", 0),
                    RouteStopDirection(Route.Id("route2"), "stop2", 1),
                ),
                PushNotificationPayload.NotificationType.Notification,
                Clock.System.now(),
            )
        assertEquals(
            DeepLinkState.Alert(alert, routeId = null, stopId = null),
            payload.getDeepLinkState(),
        )
    }

    @Test
    fun `isStillActive all clear`() {
        val payload =
            PushNotificationPayload(
                AlertSummary(Alert.Effect.Suspension, update = AlertSummary.Update.AllClear),
                "alert",
                listOf(RouteStopDirection(Route.Id("route"), "stop", 0)),
                PushNotificationPayload.NotificationType.Notification,
                Clock.System.now(),
            )
        assertEquals(
            PushNotificationPayload.StillActive.AllClear,
            payload.isStillActive(EasternTimeInstant.now()),
        )
    }

    @Test
    fun `isStillActive recurring through tomorrow`() {
        val time = EasternTimeInstant(2026, Month.FEBRUARY, 17, 14, 32)
        val payload =
            PushNotificationPayload(
                AlertSummary(
                    Alert.Effect.Suspension,
                    recurrence =
                        AlertSummary.Recurrence.Daily(ending = AlertSummary.Timeframe.Tomorrow),
                ),
                "alert",
                listOf(RouteStopDirection(Route.Id("route"), "stop", 0)),
                PushNotificationPayload.NotificationType.Notification,
                time.instant,
            )
        assertEquals(PushNotificationPayload.StillActive.Yes, payload.isStillActive(time))
        assertEquals(
            PushNotificationPayload.StillActive.Unknown,
            payload.isStillActive(time + 1.days),
        )
        assertEquals(PushNotificationPayload.StillActive.No, payload.isStillActive(time + 2.days))
    }

    @Test
    fun `isStillActive recurring through this week`() {
        val time = EasternTimeInstant(2026, Month.FEBRUARY, 17, 14, 32)
        val payload =
            PushNotificationPayload(
                AlertSummary(
                    Alert.Effect.Suspension,
                    recurrence =
                        AlertSummary.Recurrence.Daily(
                            ending = AlertSummary.Timeframe.ThisWeek(time)
                        ),
                ),
                "alert",
                listOf(RouteStopDirection(Route.Id("route"), "stop", 0)),
                PushNotificationPayload.NotificationType.Notification,
                Clock.System.now(),
            )
        assertEquals(
            PushNotificationPayload.StillActive.Yes,
            payload.isStillActive(time - 1.seconds),
        )
        assertEquals(
            PushNotificationPayload.StillActive.No,
            payload.isStillActive(time + 1.seconds),
        )
    }

    @Test
    fun `isStillActive recurring through later date`() {
        val time = EasternTimeInstant(2026, Month.FEBRUARY, 17, 14, 32)
        val payload =
            PushNotificationPayload(
                AlertSummary(
                    Alert.Effect.Suspension,
                    recurrence =
                        AlertSummary.Recurrence.Daily(
                            ending = AlertSummary.Timeframe.LaterDate(time)
                        ),
                ),
                "alert",
                listOf(RouteStopDirection(Route.Id("route"), "stop", 0)),
                PushNotificationPayload.NotificationType.Notification,
                Clock.System.now(),
            )
        assertEquals(
            PushNotificationPayload.StillActive.Yes,
            payload.isStillActive(time - 1.seconds),
        )
        assertEquals(
            PushNotificationPayload.StillActive.No,
            payload.isStillActive(time + 1.seconds),
        )
    }

    @Test
    fun `isStillActive ending unknown`() {
        val payload =
            PushNotificationPayload(
                AlertSummary(
                    Alert.Effect.Suspension,
                    timeframe = AlertSummary.Timeframe.UntilFurtherNotice,
                ),
                "alert",
                listOf(RouteStopDirection(Route.Id("route"), "stop", 0)),
                PushNotificationPayload.NotificationType.Notification,
                Clock.System.now(),
            )
        assertEquals(
            PushNotificationPayload.StillActive.Unknown,
            payload.isStillActive(EasternTimeInstant.now()),
        )
    }

    @Test
    fun `isStillActive ending later today`() {
        val time = EasternTimeInstant(2026, Month.FEBRUARY, 17, 14, 32)
        val payload =
            PushNotificationPayload(
                AlertSummary(
                    Alert.Effect.Suspension,
                    timeframe = AlertSummary.Timeframe.Time(time),
                ),
                "alert",
                listOf(RouteStopDirection(Route.Id("route"), "stop", 0)),
                PushNotificationPayload.NotificationType.Notification,
                Clock.System.now(),
            )
        assertEquals(
            PushNotificationPayload.StillActive.Yes,
            payload.isStillActive(time - 1.seconds),
        )
        assertEquals(
            PushNotificationPayload.StillActive.No,
            payload.isStillActive(time + 1.seconds),
        )
    }

    @Test
    fun `isStillActive ending end of service`() {
        val time = EasternTimeInstant(2026, Month.FEBRUARY, 17, 14, 32)
        val payload =
            PushNotificationPayload(
                AlertSummary(
                    Alert.Effect.Suspension,
                    timeframe = AlertSummary.Timeframe.EndOfService,
                ),
                "alert",
                listOf(RouteStopDirection(Route.Id("route"), "stop", 0)),
                PushNotificationPayload.NotificationType.Notification,
                time.instant,
            )
        assertEquals(
            PushNotificationPayload.StillActive.Yes,
            payload.isStillActive(EasternTimeInstant(2026, Month.FEBRUARY, 18, 2, 59)),
        )
        assertEquals(
            PushNotificationPayload.StillActive.No,
            payload.isStillActive(EasternTimeInstant(2026, Month.FEBRUARY, 18, 3, 1)),
        )
    }

    @Test
    fun `isStillActive ending tomorrow`() {
        val time = EasternTimeInstant(2026, Month.FEBRUARY, 17, 14, 32)
        val payload =
            PushNotificationPayload(
                AlertSummary(Alert.Effect.Suspension, timeframe = AlertSummary.Timeframe.Tomorrow),
                "alert",
                listOf(RouteStopDirection(Route.Id("route"), "stop", 0)),
                PushNotificationPayload.NotificationType.Notification,
                time.instant,
            )
        assertEquals(PushNotificationPayload.StillActive.Yes, payload.isStillActive(time))
        assertEquals(
            PushNotificationPayload.StillActive.Unknown,
            payload.isStillActive(time + 1.days),
        )
        assertEquals(PushNotificationPayload.StillActive.No, payload.isStillActive(time + 2.days))
    }

    @Test
    fun `isStillActive ending this week`() {
        val time = EasternTimeInstant(2026, Month.FEBRUARY, 17, 14, 32)
        val payload =
            PushNotificationPayload(
                AlertSummary(
                    Alert.Effect.Suspension,
                    timeframe = AlertSummary.Timeframe.ThisWeek(time),
                ),
                "alert",
                listOf(RouteStopDirection(Route.Id("route"), "stop", 0)),
                PushNotificationPayload.NotificationType.Notification,
                Clock.System.now(),
            )
        assertEquals(
            PushNotificationPayload.StillActive.Yes,
            payload.isStillActive(time - 1.seconds),
        )
        assertEquals(
            PushNotificationPayload.StillActive.No,
            payload.isStillActive(time + 1.seconds),
        )
    }

    @Test
    fun `isStillActive ending later date`() {
        val time = EasternTimeInstant(2026, Month.FEBRUARY, 17, 14, 32)
        val payload =
            PushNotificationPayload(
                AlertSummary(
                    Alert.Effect.Suspension,
                    timeframe = AlertSummary.Timeframe.LaterDate(time),
                ),
                "alert",
                listOf(RouteStopDirection(Route.Id("route"), "stop", 0)),
                PushNotificationPayload.NotificationType.Notification,
                Clock.System.now(),
            )
        assertEquals(
            PushNotificationPayload.StillActive.Yes,
            payload.isStillActive(time - 1.seconds),
        )
        assertEquals(
            PushNotificationPayload.StillActive.No,
            payload.isStillActive(time + 1.seconds),
        )
    }

    @Test
    fun `isStillActive starting later today`() {
        val time = EasternTimeInstant(2026, Month.FEBRUARY, 17, 14, 32)
        val payload =
            PushNotificationPayload(
                AlertSummary(
                    Alert.Effect.Suspension,
                    timeframe = AlertSummary.Timeframe.StartingLaterToday(time),
                ),
                "alert",
                listOf(RouteStopDirection(Route.Id("route"), "stop", 0)),
                PushNotificationPayload.NotificationType.Notification,
                Clock.System.now(),
            )
        assertEquals(
            PushNotificationPayload.StillActive.Reminder,
            payload.isStillActive(time - 1.seconds),
        )
        assertEquals(
            PushNotificationPayload.StillActive.Reminder,
            payload.isStillActive(time + 1.seconds),
        )
    }

    @Test
    fun `isStillActive starting tomorrow`() {
        val time = EasternTimeInstant(2026, Month.FEBRUARY, 17, 14, 32)
        val payload =
            PushNotificationPayload(
                AlertSummary(
                    Alert.Effect.Suspension,
                    timeframe = AlertSummary.Timeframe.StartingTomorrow,
                ),
                "alert",
                listOf(RouteStopDirection(Route.Id("route"), "stop", 0)),
                PushNotificationPayload.NotificationType.Notification,
                time.instant,
            )
        assertEquals(
            PushNotificationPayload.StillActive.Reminder,
            payload.isStillActive(time - 1.seconds),
        )
        assertEquals(
            PushNotificationPayload.StillActive.Reminder,
            payload.isStillActive(time + 1.seconds),
        )
    }

    @Test
    fun `isStillActive time range`() {
        val startTime = EasternTimeInstant(2026, Month.FEBRUARY, 17, 14, 32)
        val endTime = EasternTimeInstant(2026, Month.FEBRUARY, 17, 14, 42)
        val payload =
            PushNotificationPayload(
                AlertSummary(
                    Alert.Effect.Suspension,
                    timeframe =
                        AlertSummary.Timeframe.TimeRange(
                            startTime = AlertSummary.Timeframe.TimeRange.Time(startTime),
                            endTime = AlertSummary.Timeframe.TimeRange.Time(endTime),
                        ),
                ),
                "alert",
                listOf(RouteStopDirection(Route.Id("route"), "stop", 0)),
                PushNotificationPayload.NotificationType.Notification,
                Clock.System.now(),
            )
        assertEquals(
            PushNotificationPayload.StillActive.No,
            payload.isStillActive(startTime - 1.seconds),
        )
        assertEquals(
            PushNotificationPayload.StillActive.Yes,
            payload.isStillActive(startTime + 1.seconds),
        )
        assertEquals(
            PushNotificationPayload.StillActive.Yes,
            payload.isStillActive(endTime - 1.seconds),
        )
        assertEquals(
            PushNotificationPayload.StillActive.No,
            payload.isStillActive(endTime + 1.seconds),
        )
    }
}
