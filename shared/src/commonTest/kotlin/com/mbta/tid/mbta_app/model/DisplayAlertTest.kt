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
    fun `cardSpec major with still some service is regular`() = runBlocking {
        assertEquals(AlertCardSpec.Regular, DisplayAlert(hereMajorNow).cardSpec(now, false))
    }

    @Test
    fun `cardSpec major with no service somehow is takeover`() = runBlocking {
        assertEquals(AlertCardSpec.Takeover, DisplayAlert(hereMajorNow).cardSpec(now, true))
    }

    @Test
    fun `cardSpec major downstream now is downstream`() = runBlocking {
        assertEquals(
            AlertCardSpec.Downstream,
            DisplayAlert(downstreamMajorNow, true).cardSpec(now, false),
        )
    }

    @Test
    fun `cardSpec major here later is regular`() = runBlocking {
        assertEquals(AlertCardSpec.Regular, DisplayAlert(hereMajorLater).cardSpec(now, false))
    }

    @Test
    fun `cardSpec minor delay is delay`() = runBlocking {
        val minorDelay = alert {
            effect = Effect.Delay
            severity = 5
            cause = Alert.Cause.SingleTracking
        }
        assertEquals(AlertCardSpec.Delay, DisplayAlert(minorDelay).cardSpec(now, false))
    }

    @Test
    fun `cardSpec elevator`() {
        assertEquals(AlertCardSpec.Elevator, DisplayAlert(hereElevatorNow).cardSpec(now, false))
    }
}
