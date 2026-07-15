package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Month
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class AlertsChannelTest {

    @Test
    fun testParseNewDataMessage() {
        val payload =
            json.encodeToString(
                buildJsonObject {
                    putJsonObject("alerts") {
                        putJsonObject("501047") {
                            put("id", "501047")
                            putJsonArray("active_period") {
                                addJsonObject {
                                    put("start", "2023-05-26T16:46:13-04:00")
                                    put("end", JsonNull)
                                }
                            }
                            put("cause", "parade")
                            put("description", "Description")
                            put("duration_certainty", "known")
                            put("effect", "station_issue")
                            put("effect_name", JsonNull)
                            put("header", "Header")
                            putJsonArray("informed_entity") {
                                addJsonObject {
                                    putJsonArray("activities") { add("board") }
                                    put("route", "Green-D")
                                    put("route_type", "light_rail")
                                    put("stop", "70511")
                                }
                                addJsonObject {
                                    putJsonArray("activities") { add("board") }
                                    put("route", "88")
                                    put("route_type", "bus")
                                    put("stop", "place-lech")
                                }
                            }
                            put("lifecycle", "ongoing")
                            put("severity", 10)

                            put("updated_at", "2023-05-26T16:46:13-04:00")
                        }
                    }
                }
            )

        val parsed = AlertsChannel.parseMessage(payload)

        assertEquals(
            AlertsStreamDataResponse(
                mapOf(
                    "501047" to
                        Alert(
                            "501047",
                            listOf(
                                Alert.ActivePeriod(
                                    EasternTimeInstant(2023, Month.MAY, 26, 16, 46, 13),
                                    null,
                                )
                            ),
                            Alert.Cause.Parade,
                            "Description",
                            Alert.DurationCertainty.Known,
                            Alert.Effect.StationIssue,
                            null,
                            "Header",
                            listOf(
                                Alert.InformedEntity(
                                    listOf(Alert.InformedEntity.Activity.Board),
                                    route = Route.Id("Green-D"),
                                    routeType = RouteType.LIGHT_RAIL,
                                    stop = "70511",
                                ),
                                Alert.InformedEntity(
                                    listOf(Alert.InformedEntity.Activity.Board),
                                    route = Route.Id("88"),
                                    routeType = RouteType.BUS,
                                    stop = "place-lech",
                                ),
                            ),
                            Alert.Lifecycle.Ongoing,
                            10,
                            EasternTimeInstant(2023, Month.MAY, 26, 16, 46, 13),
                        )
                )
            ),
            parsed,
        )
    }

    @Test
    fun testParseUnknownCauseAndEffect() {
        val payload =
            json.encodeToString(
                buildJsonObject {
                    putJsonObject("alerts") {
                        putJsonObject("501047") {
                            put("id", "501047")
                            putJsonArray("active_period") {
                                addJsonObject {
                                    put("start", "2023-05-26T16:46:13-04:00")
                                    put("end", JsonNull)
                                }
                            }
                            put("cause", "gravitational_anomalies")
                            put("description", "Description")
                            put("duration_certainty", "beyond_mortal_comprehension")
                            put("effect", "hover_trains")
                            put("effect_name", JsonNull)
                            put("header", "Header")
                            putJsonArray("informed_entity") {
                                addJsonObject {
                                    putJsonArray("activities") { add("board") }
                                    put("route", "Red")
                                    put("route_type", "heavy_rail")
                                    put("stop", "place-pktrm")
                                }
                            }
                            put("lifecycle", "ongoing")
                            put("severity", 10)
                            put("updated_at", "2023-05-26T16:46:13-04:00")
                        }
                    }
                }
            )

        val parsed = AlertsChannel.parseMessage(payload)

        assertEquals(
            AlertsStreamDataResponse(
                mapOf(
                    "501047" to
                        Alert(
                            "501047",
                            listOf(
                                Alert.ActivePeriod(
                                    EasternTimeInstant(2023, Month.MAY, 26, 16, 46, 13),
                                    null,
                                )
                            ),
                            Alert.Cause.UnknownCause,
                            "Description",
                            Alert.DurationCertainty.Unknown,
                            Alert.Effect.UnknownEffect,
                            null,
                            "Header",
                            listOf(
                                Alert.InformedEntity(
                                    listOf(Alert.InformedEntity.Activity.Board),
                                    route = Route.Id("Red"),
                                    routeType = RouteType.HEAVY_RAIL,
                                    stop = "place-pktrm",
                                )
                            ),
                            Alert.Lifecycle.Ongoing,
                            10,
                            EasternTimeInstant(2023, Month.MAY, 26, 16, 46, 13),
                        )
                )
            ),
            parsed,
        )
    }
}
