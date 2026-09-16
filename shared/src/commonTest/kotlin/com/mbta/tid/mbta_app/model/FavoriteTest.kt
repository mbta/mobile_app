package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.buildFavorites
import kotlin.test.Test
import kotlin.test.assertEquals
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

    fun assertWindowEquals(expected: Window, actual: Window) {
        assertEquals(expected.startTime, actual.startTime)
        assertEquals(expected.endTime, actual.endTime)
        assertEquals(expected.daysOfWeek, actual.daysOfWeek)
    }

    fun assertFavoritesEquals(expected: Favorites, actual: Favorites) {
        assertEquals(expected.routeStopDirection.size, actual.routeStopDirection.size)
        expected.routeStopDirection.forEach { (rsd, expectedSettings) ->
            val actualSettings = actual.routeStopDirection[rsd]
            assertEquals(
                expectedSettings.notifications.enabled,
                actualSettings?.notifications?.enabled,
            )
            assertEquals(
                expectedSettings.notifications.windows.size,
                actualSettings?.notifications?.windows?.size,
            )
            expectedSettings.notifications.windows.forEachIndexed { index, expectedWindow ->
                val actualWindow = actualSettings?.notifications?.windows?.get(index)
                assertWindowEquals(expectedWindow, actualWindow!!)
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
    fun `customFromCurrentTime maxes out before midnight`() {
        val now = EasternTimeInstant(LocalDateTime(2026, 8, 27, 23, 30))

        assertWindowEquals(
            Window(
                LocalTime(23, 0),
                LocalTime(23, 59),
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
    fun `minimumEndTime advances by one minute up until end of day`() {
        assertEquals(
            LocalTime(8, 1),
            Window.minimumEndTime(LocalTime(8, 0)),
        )
        assertEquals(
            LocalTime(9, 0),
            Window.minimumEndTime(LocalTime(8, 59)),
        )
        assertEquals(
            LocalTime(23, 59),
            Window.minimumEndTime(LocalTime(23, 59)),
        )
    }

    @Test
    fun `safeEndTime keeps valid end times and adjusts invalid ones`() {
        assertEquals(
            LocalTime(9, 30),
            Window.safeEndTime(
                startTime = LocalTime(8, 30),
                endTime = LocalTime(9, 30),
            ),
        )
        assertEquals(
            LocalTime(9, 30),
            Window.safeEndTime(
                startTime = LocalTime(8, 30),
                endTime = LocalTime(7, 45),
            ),
        )
        assertEquals(
            LocalTime(23, 59),
            Window.safeEndTime(
                startTime = LocalTime(23, 30),
                endTime = LocalTime(23, 0),
            ),
        )
    }
}
