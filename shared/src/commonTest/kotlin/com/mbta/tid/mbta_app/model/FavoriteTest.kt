package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.utils.buildFavorites
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class FavoriteTest {
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
                    RouteStopDirection("route1", "stop1", 0) to FavoriteSettings(),
                    RouteStopDirection("route2", "stop2", 1) to FavoriteSettings(),
                )
            ),
            newFavorites,
        )
    }

    @Test
    fun `parses and serializes post-notifications format`() {
        val favorites = buildFavorites {
            routeStopDirection("route1", "stop1", 0) {
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
            routeStopDirection("route2", "stop2", 1)
        }
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
                    putJsonObject("second") {
                        putJsonObject("notifications") {
                            put("enabled", false)
                            putJsonArray("windows") {}
                        }
                    }
                }
            }
        }
        assertEquals(serialized, json.encodeToJsonElement(favorites))
        assertEquals(favorites, json.decodeFromJsonElement(serialized))
    }
}
