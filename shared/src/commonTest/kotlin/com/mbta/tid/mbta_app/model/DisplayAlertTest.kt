package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.Alert.Effect
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.alert
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.runBlocking

class DisplayAlertTest {

    val objects = ObjectCollectionBuilder()
    val route = LineOrRoute.Route(objects.route())
    val stop = objects.stop()
    val now = EasternTimeInstant.now()

    val hereMajorNow =
        objects.alert {
            id = "hereMajorNow"
            effect = Effect.Shuttle
            activePeriod = mutableListOf(Alert.ActivePeriod(now.minus(10.minutes), null))
        }

    val hereElevatorNow =
        objects.alert {
            id = "hereElevatorNow"
            effect = Effect.ElevatorClosure
            activePeriod = mutableListOf(Alert.ActivePeriod(now.minus(10.minutes), null))
        }

    val hereMajorLater =
        objects.alert {
            id = "hereMajorLater"
            effect = Effect.Shuttle
            activePeriod = mutableListOf(Alert.ActivePeriod(now.plus(10.minutes), null))
        }

    val downstreamMajorNow =
        objects.alert {
            id = "downstreamMajorNow"
            effect = Effect.Shuttle
            activePeriod = mutableListOf(Alert.ActivePeriod(now.minus(10.minutes), null))
        }

    @Test
    fun `cardSpec major with still some service somehow is regular`() = runBlocking {
        assertEquals(AlertCardSpec.Basic, DisplayAlert(hereMajorNow).cardSpec(now, false, null))
    }

    @Test
    fun `cardSpec major with no service is takeover`() = runBlocking {
        assertEquals(AlertCardSpec.Takeover, DisplayAlert(hereMajorNow).cardSpec(now, true, null))
    }

    @Test
    fun `cardSpec major for selected trip is takeover`() = runBlocking {
        val tripAlert =
            objects.alert {
                id = "hereForTargetTrip"
                effect = Effect.Shuttle
                activePeriod = mutableListOf(Alert.ActivePeriod(now.minus(10.minutes), null))
                informedEntity =
                    mutableListOf(
                        Alert.InformedEntity(
                            trip = "trip1",
                            activities =
                                listOf(
                                    Alert.InformedEntity.Activity.Board,
                                    Alert.InformedEntity.Activity.Exit,
                                    Alert.InformedEntity.Activity.Ride,
                                ),
                        )
                    )
            }
        assertEquals(AlertCardSpec.Takeover, DisplayAlert(tripAlert).cardSpec(now, true, "trip1"))
    }

    @Test
    fun `cardSpec major downstream now is downstream`() = runBlocking {
        assertEquals(
            AlertCardSpec.Downstream,
            DisplayAlert(downstreamMajorNow, true).cardSpec(now, false, null),
        )
    }

    @Test
    fun `cardSpec major here later is regular`() = runBlocking {
        assertEquals(AlertCardSpec.Basic, DisplayAlert(hereMajorLater).cardSpec(now, false, null))
    }

    @Test
    fun `cardSpec minor delay is delay`() = runBlocking {
        val minorDelay = alert {
            effect = Effect.Delay
            severity = 5
            cause = Alert.Cause.SingleTracking
        }
        assertEquals(AlertCardSpec.Delay, DisplayAlert(minorDelay).cardSpec(now, false, null))
    }

    @Test
    fun `cardSpec elevator`() {
        assertEquals(
            AlertCardSpec.Elevator,
            DisplayAlert(hereElevatorNow).cardSpec(now, false, null),
        )
    }
}
