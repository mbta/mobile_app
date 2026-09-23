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
                )
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
    fun `safeEndTime keeps valid end times and adjusts invalid ones to the next quarter hour`() {
        assertEquals(
            LocalTime(9, 30),
            Window.safeEndTime(
                startTime = LocalTime(8, 30),
                endTime = LocalTime(9, 30),
            ),
        )
        // end time is before start time and after the 3am service boundary, so it's invalid and
        // gets adjusted to the next 15 minute increment after the start time
        assertEquals(
            LocalTime(8, 45),
            Window.safeEndTime(
                startTime = LocalTime(8, 30),
                endTime = LocalTime(7, 45),
            ),
        )
        assertEquals(
            LocalTime(23, 45),
            Window.safeEndTime(
                startTime = LocalTime(23, 30),
                endTime = LocalTime(23, 0),
            ),
        )
        // end time before the 3am boundary is treated as a valid next-day end time
        assertEquals(
            LocalTime(2, 0),
            Window.safeEndTime(
                startTime = LocalTime(23, 30),
                endTime = LocalTime(2, 0),
            ),
        )
    }

    @Test
    fun `safeEndTime rounds up to service end if boolean is passed`() {
        assertEquals(
            LocalTime(3, 0),
            Window.safeEndTime(
                startTime = LocalTime(8, 30),
                endTime = LocalTime(8, 30),
                roundUp = true,
            ),
        )
        assertEquals(
            LocalTime(3, 0),
            Window.safeEndTime(
                startTime = LocalTime(8, 30),
                endTime = LocalTime(4, 0),
                roundUp = true,
            ),
        )
    }
}
