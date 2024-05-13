package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Instant

class TripDetailsStopListTest {
    @Test
    fun `handles stop IDs after all predictions are gone`() {
        val objects = ObjectCollectionBuilder()
        val predictions = PredictionsStreamDataResponse(objects)

        val stop1 = objects.stop()
        val stop2 = objects.stop()
        val stop3 = objects.stop()
        val stop4 = objects.stop()
        val schedules =
            TripSchedulesResponse.StopIds(listOf(stop1.id, stop2.id, stop3.id, stop4.id))

        val globalData = GlobalResponse(objects, emptyMap())

        val list = TripDetailsStopList.fromPieces(schedules, predictions, globalData)

        assertEquals(
            TripDetailsStopList(
                listOf(
                    TripDetailsStopList.Entry(stop1, 996, null, null, null),
                    TripDetailsStopList.Entry(stop2, 997, null, null, null),
                    TripDetailsStopList.Entry(stop3, 998, null, null, null),
                    TripDetailsStopList.Entry(stop4, 999, null, null, null),
                )
            ),
            list
        )
    }

    @Test
    fun `handles wacky Park St mismatch`() {
        val objects = ObjectCollectionBuilder()

        val prediction =
            objects.prediction {
                arrivalTime = Instant.parse("2024-05-09T21:28:23Z")
                status = "Stopped at station"
                stopSequence = 600
                stopId = "71199"
            }

        val predictions = PredictionsStreamDataResponse(objects)

        val schedules =
            TripSchedulesResponse.StopIds(
                listOf(
                    // Boston College through Copley elided for simplicity
                    "70156",
                    "70158",
                    "70200",
                    "70201"
                )
            )

        val arlington =
            objects.stop {
                id = "70156"
                parentStationId = "place-armnl"
            }
        val boylston =
            objects.stop {
                id = "70158"
                parentStationId = "place-boyls"
            }
        objects.stop {
            id = "70200"
            parentStationId = "place-pktrm"
        }
        val governmentCenter =
            objects.stop {
                id = "70201"
                name = "Government Center"
                parentStationId = "place-gover"
            }
        val alsoParkStreetSomewhat =
            objects.stop {
                id = "71199"
                description = "Park Street - Green Line - Drop-off Only"
                name = "Park Street"
                parentStationId = "place-pktrm"
            }

        val globalData = GlobalResponse(objects, emptyMap())

        val list = TripDetailsStopList.fromPieces(schedules, predictions, globalData)

        assertEquals(
            TripDetailsStopList(
                listOf(
                    TripDetailsStopList.Entry(arlington, 598, null, null, null),
                    TripDetailsStopList.Entry(boylston, 599, null, null, null),
                    TripDetailsStopList.Entry(alsoParkStreetSomewhat, 600, null, prediction, null),
                    TripDetailsStopList.Entry(governmentCenter, 601, null, null, null),
                )
            ),
            list
        )
    }

    @Test
    fun `handles this case`() {
        val objects = ObjectCollectionBuilder()

        val p1 =
            objects.prediction {
                arrivalTime = Instant.parse("2024-05-10T15:31:05Z")
                departureTime = Instant.parse("2024-05-10T15:32:12Z")
                stopSequence = 600
                stopId = "70200"
            }
        val p2 =
            objects.prediction {
                arrivalTime = Instant.parse("2024-05-10T15:31:08Z")
                departureTime = null
                stopSequence = 600
                stopId = "71199"
            }
        val p3 =
            objects.prediction {
                arrivalTime = Instant.parse("2024-05-10T15:33:07Z")
                departureTime = null
                stopSequence = 610
                stopId = "70201"
            }

        val predictions = PredictionsStreamDataResponse(objects)

        val schedules = TripSchedulesResponse.StopIds(listOf("70158", "70200", "70201"))

        val boylston =
            objects.stop {
                id = "70158"
                parentStationId = "place-boyls"
            }
        val parkStreet =
            objects.stop {
                id = "70200"
                parentStationId = "place-pktrm"
            }
        val governmentCenter =
            objects.stop {
                id = "70201"
                name = "Government Center"
                parentStationId = "place-gover"
            }
        val notQuiteParkStreet =
            objects.stop {
                id = "71199"
                description = "Park Street - Green Line - Drop-off Only"
                name = "Park Street"
                parentStationId = "place-pktrm"
            }

        val globalData = GlobalResponse(objects, emptyMap())

        val list = TripDetailsStopList.fromPieces(schedules, predictions, globalData)

        assertEquals(
            TripDetailsStopList(
                listOf(
                    TripDetailsStopList.Entry(boylston, 590, null, null, null),
                    TripDetailsStopList.Entry(parkStreet, 600, null, p1, null),
                    TripDetailsStopList.Entry(governmentCenter, 610, null, p3, null),
                )
            ),
            list
        )
    }
}
