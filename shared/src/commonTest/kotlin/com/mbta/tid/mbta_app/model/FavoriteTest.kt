package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.buildFavorites
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

private typealias Window = FavoriteSettings.Notifications.Window

class FavoriteTest {

    companion object {
        fun assertWindowEquals(expected: Window, actual: Window?) {
            if (actual == null) fail("Actual window is null, expected: $expected")
            assertEquals(expected.startTime, actual.startTime)
            assertEquals(expected.endTime, actual.endTime)
            assertEquals(expected.daysOfWeek, actual.daysOfWeek)
        }

        fun assertWindowsEquals(expected: List<Window>, actual: List<Window>?) {
            actual?.zip(expected)?.forEach { (expectedWindow, actualWindow) ->
                assertWindowEquals(expectedWindow, actualWindow)
            } ?: fail("Actual windows list is null, expected: $expected")
        }

        fun assertFavoritesEquals(expected: Favorites, actual: Favorites?) {
            if (actual == null) fail("Actual favorites is null, expected: $expected")
            assertEquals(expected.routeStopDirection.size, actual.routeStopDirection.size)
            expected.routeStopDirection.forEach { (rsd, expectedSettings) ->
                val actualSettings = actual.routeStopDirection[rsd]
                assertEquals(
                    expectedSettings.notifications.enabled,
                    actualSettings?.notifications?.enabled,
                )
                assertWindowsEquals(
                    expectedSettings.notifications.windows,
                    actualSettings?.notifications?.windows,
                )
            }
        }
    }

    @Test
    fun `parses pre-notifications format`() {
        val oldFavorites = buildJsonObject {
            putJsonArray("routeStopDirection") {
                addJsonObject {
                    put("route", "route1")
                    put("stop", "stop1")
                    put("direction", 0)
                }
                addJsonObject {
                    put("route", "route2")
                    put("stop", "stop2")
                    put("direction", 1)
                }
            }
        }
        val newFavorites = json.decodeFromJsonElement<Favorites>(oldFavorites)
        assertEquals(
            Favorites(
                mapOf(
                    RouteStopDirection(Route.Id("route1"), "stop1", 0) to FavoriteSettings(),
                    RouteStopDirection(Route.Id("route2"), "stop2", 1) to FavoriteSettings(),
                ),
            ),
            newFavorites,
        )
    }

    @Test
    fun `parses and serializes post-notifications format`() {
        val favorites = buildFavorites {
            routeStopDirection(Route.Id("route1"), "stop1", 0) {
                notifications {
                    enabled = true
                    window(
                        LocalTime(8, 0),
                        LocalTime(9, 0),
                        setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
                    )
                    window(LocalTime(10, 0), LocalTime(13, 0), setOf(DayOfWeek.SATURDAY))
                }
            }
            routeStopDirection(Route.Id("route2"), "stop2", 1)
        }
        val windows = favorites.routeStopDirection.entries.first().value.notifications.windows
        val serialized = buildJsonObject {
            putJsonArray("postNotificationsRSDs") {
                addJsonObject {
                    putJsonObject("first") {
                        put("route", "route1")
                        put("stop", "stop1")
                        put("direction", 0)
                    }
                    putJsonObject("second") {
                        putJsonObject("notifications") {
                            put("enabled", true)
                            putJsonArray("windows") {
                                addJsonObject {
                                    put("startTime", "08:00")
                                    put("endTime", "09:00")
                                    putJsonArray("daysOfWeek") {
                                        add("MONDAY")
                                        add("TUESDAY")
                                        add("WEDNESDAY")
                                    }
                                }
                                addJsonObject {
                                    put("startTime", "10:00")
                                    put("endTime", "13:00")
                                    putJsonArray("daysOfWeek") { add("SATURDAY") }
                                }
                            }
                        }
                    }
                }
                addJsonObject {
                    putJsonObject("first") {
                        put("route", "route2")
                        put("stop", "stop2")
                        put("direction", 1)
                    }
                    putJsonObject("second") {}
                }
            }
        }
        assertEquals(serialized, json.encodeToJsonElement(favorites))
        assertFavoritesEquals(favorites, json.decodeFromJsonElement(serialized))
    }

    @Test
    fun `defaultFromCurrentTime returns the matching preset`() {
        assertWindowEquals(
            Window(Preset.Morning, Window.weekdays),
            Window.defaultFromCurrentTime(EasternTimeInstant(LocalDateTime(2026, 8, 27, 7, 30))),
        )
        assertWindowEquals(
            Window(Preset.Midday, Window.weekdays),
            Window.defaultFromCurrentTime(EasternTimeInstant(LocalDateTime(2026, 8, 27, 12, 30))),
        )

        assertWindowEquals(
            Window(Preset.Evening, Window.weekdays),
            Window.defaultFromCurrentTime(EasternTimeInstant(LocalDateTime(2026, 8, 27, 18, 30))),
        )

        assertWindowEquals(
            Window(Preset.AllDay, Window.weekdays),
            Window.defaultFromCurrentTime(EasternTimeInstant(LocalDateTime(2026, 8, 27, 21, 30))),
        )

        assertWindowEquals(
            Window(Preset.AllDay, Window.weekend),
            Window.defaultFromCurrentTime(EasternTimeInstant(LocalDateTime(2026, 8, 30, 21, 30))),
        )
    }

    @Test
    fun `customFromCurrentTime rounds to the current hour`() {
        val now = EasternTimeInstant(LocalDateTime(2026, 8, 27, 9, 30))

        assertWindowEquals(
            Window(
                LocalTime(9, 0),
                LocalTime(10, 0),
                setOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                ),
            ),
            Window.customFromCurrentTime(now),
        )
    }

    @Test
    fun `customFromCurrentTime wraps into early morning past midnight up to the 3am service boundary`() {
        val now = EasternTimeInstant(LocalDateTime(2026, 8, 27, 23, 30))

        assertWindowEquals(
            Window(
                LocalTime(23, 0),
                LocalTime(1, 0),
                setOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                ),
            ),
            Window.customFromCurrentTime(now),
        )
    }

    @Test
    fun `adjustedEndTime keeps valid end times and adjusts invalid ones`() {
        // start time is the 3am service boundary, so any end time is valid
        assertEquals(
            LocalTime(1, 0),
            Window.adjustedEndTime(
                startTime = LocalTime(3, 0),
                endTime = LocalTime(1, 0),
            ),
        )
        assertEquals(
            LocalTime(8, 0),
            Window.adjustedEndTime(
                startTime = LocalTime(3, 0),
                endTime = LocalTime(8, 0),
            ),
        )
        assertEquals(
            LocalTime(3, 0),
            Window.adjustedEndTime(
                startTime = LocalTime(3, 0),
                endTime = LocalTime(3, 0),
            ),
        )
        // end time is the 3am service boundary, so it is valid for any start time
        assertEquals(
            LocalTime(3, 0),
            Window.adjustedEndTime(
                startTime = LocalTime(1, 0),
                endTime = LocalTime(3, 0),
            ),
        )
        assertEquals(
            LocalTime(3, 0),
            Window.adjustedEndTime(
                startTime = LocalTime(8, 0),
                endTime = LocalTime(3, 0),
            ),
        )
        // start time is after midnight and end time is during the day, so the window would cross
        // the 3am service boundary and the end time is set to the boundary
        assertEquals(
            LocalTime(3, 0),
            Window.adjustedEndTime(
                startTime = LocalTime(1, 0),
                endTime = LocalTime(8, 0),
            ),
        )
        assertEquals(
            LocalTime(3, 0),
            Window.adjustedEndTime(
                startTime = LocalTime(2, 0),
                endTime = LocalTime(3, 15),
            ),
        )
        // start and end times are both after midnight
        assertEquals(
            LocalTime(2, 0),
            Window.adjustedEndTime(
                startTime = LocalTime(1, 0),
                endTime = LocalTime(2, 0),
            ),
        )
        // end time is not after the start time, so it is set to 15 minutes after the start time
        assertEquals(
            LocalTime(2, 15),
            Window.adjustedEndTime(
                startTime = LocalTime(2, 0),
                endTime = LocalTime(1, 0),
            ),
        )
        assertEquals(
            LocalTime(2, 15),
            Window.adjustedEndTime(
                startTime = LocalTime(2, 0),
                endTime = LocalTime(2, 0),
            ),
        )
        assertEquals(
            LocalTime(3, 0),
            Window.adjustedEndTime(
                startTime = LocalTime(2, 45),
                endTime = LocalTime(0, 0),
            ),
        )
        // start and end times are both during the day
        assertEquals(
            LocalTime(17, 0),
            Window.adjustedEndTime(
                startTime = LocalTime(8, 0),
                endTime = LocalTime(17, 0),
            ),
        )
        // end time is not after the start time, so it is set to 15 minutes after the start time
        assertEquals(
            LocalTime(17, 15),
            Window.adjustedEndTime(
                startTime = LocalTime(17, 0),
                endTime = LocalTime(8, 0),
            ),
        )
        assertEquals(
            LocalTime(0, 0),
            Window.adjustedEndTime(
                startTime = LocalTime(23, 45),
                endTime = LocalTime(23, 45),
            ),
        )
        // start time is during the day and end time is after midnight, so it is a valid next-day
        // end time
        assertEquals(
            LocalTime(0, 30),
            Window.adjustedEndTime(
                startTime = LocalTime(17, 0),
                endTime = LocalTime(0, 30),
            ),
        )
    }

    @Test
    fun `adjustedStartTime keeps valid start times and adjusts invalid ones`() {
        // end time is the 3am service boundary, so any start time is valid
        assertEquals(
            LocalTime(1, 0),
            Window.adjustedStartTime(
                startTime = LocalTime(1, 0),
                endTime = LocalTime(3, 0),
            ),
        )
        assertEquals(
            LocalTime(8, 0),
            Window.adjustedStartTime(
                startTime = LocalTime(8, 0),
                endTime = LocalTime(3, 0),
            ),
        )
        assertEquals(
            LocalTime(3, 0),
            Window.adjustedStartTime(
                startTime = LocalTime(3, 0),
                endTime = LocalTime(3, 0),
            ),
        )
        // start time is the 3am service boundary, so it is valid for any end time
        assertEquals(
            LocalTime(3, 0),
            Window.adjustedStartTime(
                startTime = LocalTime(3, 0),
                endTime = LocalTime(1, 0),
            ),
        )
        assertEquals(
            LocalTime(3, 0),
            Window.adjustedStartTime(
                startTime = LocalTime(3, 0),
                endTime = LocalTime(8, 0),
            ),
        )
        // start time is during the day and end time is after midnight, so it is a valid next-day
        // end time
        assertEquals(
            LocalTime(8, 0),
            Window.adjustedStartTime(
                startTime = LocalTime(8, 0),
                endTime = LocalTime(1, 0),
            ),
        )
        // start and end times are both after midnight
        assertEquals(
            LocalTime(1, 0),
            Window.adjustedStartTime(
                startTime = LocalTime(1, 0),
                endTime = LocalTime(2, 0),
            ),
        )
        // start time is not before the end time, so it is set to 15 minutes before the end time
        assertEquals(
            LocalTime(1, 45),
            Window.adjustedStartTime(
                startTime = LocalTime(2, 30),
                endTime = LocalTime(2, 0),
            ),
        )
        assertEquals(
            LocalTime(1, 45),
            Window.adjustedStartTime(
                startTime = LocalTime(2, 0),
                endTime = LocalTime(2, 0),
            ),
        )
        assertEquals(
            LocalTime(23, 45),
            Window.adjustedStartTime(
                startTime = LocalTime(1, 0),
                endTime = LocalTime(0, 0),
            ),
        )
        // start and end times are both during the day
        assertEquals(
            LocalTime(8, 0),
            Window.adjustedStartTime(
                startTime = LocalTime(8, 0),
                endTime = LocalTime(17, 0),
            ),
        )
        // start time is not before the end time, so it is set to 15 minutes before the end time
        assertEquals(
            LocalTime(7, 45),
            Window.adjustedStartTime(
                startTime = LocalTime(17, 0),
                endTime = LocalTime(8, 0),
            ),
        )
        assertEquals(
            LocalTime(7, 45),
            Window.adjustedStartTime(
                startTime = LocalTime(8, 0),
                endTime = LocalTime(8, 0),
            ),
        )
        assertEquals(
            LocalTime(3, 0),
            Window.adjustedStartTime(
                startTime = LocalTime(8, 0),
                endTime = LocalTime(3, 15),
            ),
        )
        // start time is after midnight and end time is during the day, so the window would cross
        // the 3am service boundary and the start time is set to the boundary
        assertEquals(
            LocalTime(3, 0),
            Window.adjustedStartTime(
                startTime = LocalTime(1, 0),
                endTime = LocalTime(17, 0),
            ),
        )
        assertEquals(
            LocalTime(3, 0),
            Window.adjustedStartTime(
                startTime = LocalTime(0, 0),
                endTime = LocalTime(23, 45),
            ),
        )
    }
}
