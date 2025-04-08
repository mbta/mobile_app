package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.serviceDate
import com.mbta.tid.mbta_app.utils.toBostonTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant

class AlertSummaryTest {
    @Test
    fun `summary is null when there is no timeframe or location`() {
        val objects = ObjectCollectionBuilder()
        val now = Clock.System.now()
        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(now.minus(1.hours), now.plus(1.hours))
                durationCertainty = Alert.DurationCertainty.Estimated
            }

        val alertSummary =
            AlertSummary.summarizing(alert, 0, emptyList(), now, GlobalResponse(objects))

        assertNull(alertSummary)
    }

    @Test
    fun `summary with later today timeframe`() {
        val objects = ObjectCollectionBuilder()
        val now = Clock.System.now()
        val endTime = now.plus(1.hours)
        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(now.minus(1.hours), endTime)
            }

        val alertSummary =
            AlertSummary.summarizing(alert, 0, emptyList(), now, GlobalResponse(objects))

        assertEquals(
            AlertSummary(alert.effect, null, AlertSummary.Timeframe.Time(endTime)),
            alertSummary
        )
    }

    @Test
    fun `summary with end of service timeframe`() {
        val objects = ObjectCollectionBuilder()
        val now = Clock.System.now()

        val tomorrow = now.toBostonTime().serviceDate.plus(DatePeriod(days = 1))
        val serviceEndTime = LocalTime(hour = 2, minute = 59)
        val endTime = tomorrow.atTime(serviceEndTime).toInstant(TimeZone.of("America/New_York"))

        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(now.minus(1.hours), endTime)
            }

        val alertSummary =
            AlertSummary.summarizing(alert, 0, emptyList(), now, GlobalResponse(objects))

        assertEquals(
            AlertSummary(alert.effect, null, AlertSummary.Timeframe.EndOfService),
            alertSummary
        )
    }

    @Test
    fun `summary with alt end of service timeframe`() {
        val objects = ObjectCollectionBuilder()
        val now = Clock.System.now()

        val tomorrow = now.toBostonTime().serviceDate.plus(DatePeriod(days = 1))
        val serviceEndTime = LocalTime(hour = 3, minute = 0)
        val endTime = tomorrow.atTime(serviceEndTime).toInstant(TimeZone.of("America/New_York"))

        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(now.minus(1.hours), endTime)
            }

        val alertSummary =
            AlertSummary.summarizing(alert, 0, emptyList(), now, GlobalResponse(objects))

        assertEquals(
            AlertSummary(alert.effect, null, AlertSummary.Timeframe.EndOfService),
            alertSummary
        )
    }

    @Test
    fun `summary with tomorrow timeframe`() {
        val objects = ObjectCollectionBuilder()
        val now = Clock.System.now()

        // Set to tomorrow's end of service, with a date of 2 days from now
        val tomorrow = now.toBostonTime().serviceDate.plus(DatePeriod(days = 2))
        val serviceEndTime = LocalTime(hour = 3, minute = 0)
        val endTime = tomorrow.atTime(serviceEndTime).toInstant(TimeZone.of("America/New_York"))

        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(now.minus(1.hours), endTime)
            }

        val alertSummary =
            AlertSummary.summarizing(alert, 0, emptyList(), now, GlobalResponse(objects))

        assertEquals(
            AlertSummary(alert.effect, null, AlertSummary.Timeframe.Tomorrow),
            alertSummary
        )
    }

    @Test
    fun `summary with this week timeframe`() {
        val objects = ObjectCollectionBuilder()
        // Fixed time so we can have a specific day of the week (wed)
        val now = Instant.fromEpochMilliseconds(1743598800000)

        val saturday = now.toBostonTime().serviceDate.plus(DatePeriod(days = 3))
        val serviceEndTime = LocalTime(hour = 5, minute = 0)
        val endTime = saturday.atTime(serviceEndTime).toInstant(TimeZone.of("America/New_York"))

        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(now.minus(1.hours), endTime)
            }

        val alertSummary =
            AlertSummary.summarizing(alert, 0, emptyList(), now, GlobalResponse(objects))

        assertEquals(
            AlertSummary(alert.effect, null, AlertSummary.Timeframe.ThisWeek(endTime)),
            alertSummary
        )
    }

    @Test
    fun `summary with later date timeframe`() {
        val objects = ObjectCollectionBuilder()
        // Fixed time so we can have a specific day of the week (wed)
        val now = Instant.fromEpochMilliseconds(1743598800000)

        val monday = now.toBostonTime().serviceDate.plus(DatePeriod(days = 5))
        val serviceEndTime = LocalTime(hour = 5, minute = 0)
        val endTime = monday.atTime(serviceEndTime).toInstant(TimeZone.of("America/New_York"))

        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(now.minus(1.hours), endTime)
            }

        val alertSummary =
            AlertSummary.summarizing(alert, 0, emptyList(), now, GlobalResponse(objects))

        assertEquals(
            AlertSummary(alert.effect, null, AlertSummary.Timeframe.LaterDate(endTime)),
            alertSummary
        )
    }

    @Test
    fun `summary with single stop`() {
        val objects = ObjectCollectionBuilder()

        val now = Clock.System.now()

        val stop =
            objects.stop {
                name = "Parent Name"
                childStop {}
            }
        val childStop = objects.stops[stop.childStopIds.first()]

        val route = objects.route {}
        val pattern = objects.routePattern(route) { directionId = 0 }
        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                durationCertainty = Alert.DurationCertainty.Estimated
                activePeriod(now.minus(1.hours), now.plus(1.hours))
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride
                    ),
                    route = route.id,
                    stop = childStop?.id
                )
            }

        val alertSummary =
            AlertSummary.summarizing(alert, 0, listOf(pattern), now, GlobalResponse(objects))

        assertEquals(
            AlertSummary(alert.effect, AlertSummary.Location.SingleStop(stop.name), null),
            alertSummary
        )
    }

    @Test
    fun `summary with successive stops`() {
        val objects = ObjectCollectionBuilder()

        val now = Clock.System.now()

        val firstStop = objects.stop { name = "First Stop" }
        val successiveStops = (1..4).map { objects.stop { name = "Successive Stop $it" } }
        val lastStop = objects.stop { name = "Last Stop" }

        val stops = listOf(firstStop) + successiveStops + listOf(lastStop)

        val route = objects.route {}
        val pattern =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip { stopIds = stops.map { it.id } }
            }
        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                durationCertainty = Alert.DurationCertainty.Estimated
                activePeriod(now.minus(1.hours), now.plus(1.hours))
                for (stop in stops) {
                    informedEntity(
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Exit,
                            Alert.InformedEntity.Activity.Ride
                        ),
                        route = route.id,
                        stop = stop.id
                    )
                }
            }

        val alertSummary =
            AlertSummary.summarizing(alert, 0, listOf(pattern), now, GlobalResponse(objects))

        assertEquals(
            AlertSummary(
                alert.effect,
                AlertSummary.Location.SuccessiveStops(firstStop.name, lastStop.name),
                null
            ),
            alertSummary
        )
    }
}
