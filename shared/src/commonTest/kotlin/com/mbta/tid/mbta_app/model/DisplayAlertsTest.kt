package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.Alert.Effect
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.runBlocking

class DisplayAlertsTest {

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
    val hereMinorNow =
        objects.alert {
            id = "hereMinorNow"
            effect = Effect.TrackChange
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
    val hereMinorLater =
        objects.alert {
            id = "hereMinorLater"
            effect = Effect.TrackChange
            activePeriod = mutableListOf(Alert.ActivePeriod(now.plus(10.minutes), null))
        }

    val hereElevatorLater =
        objects.alert {
            id = "hereElevatorLater"
            effect = Effect.ElevatorClosure
            activePeriod = mutableListOf(Alert.ActivePeriod(now.plus(10.minutes), null))
        }

    val downstreamMajorNow =
        objects.alert {
            id = "downstreamMajorNow"
            effect = Effect.Shuttle
            activePeriod = mutableListOf(Alert.ActivePeriod(now.minus(10.minutes), null))
        }
    val downstreamMinorNow =
        objects.alert {
            id = "downstreamMinorNow"
            effect = Effect.TrackChange
            activePeriod = mutableListOf(Alert.ActivePeriod(now.minus(10.minutes), null))
        }

    val downstreamMajorLater =
        objects.alert {
            id = "downstreamMajorLater"
            effect = Effect.Shuttle
            activePeriod = mutableListOf(Alert.ActivePeriod(now.plus(10.minutes), null))
        }
    val downstreamMinorLater =
        objects.alert {
            id = "downstreamMinorLater"
            effect = Effect.TrackChange
            activePeriod = mutableListOf(Alert.ActivePeriod(now.plus(10.minutes), null))
        }

    val downstreamMinorEvenLater =
        objects.alert {
            id = "downstreamMinorEvenLater"
            effect = Effect.TrackChange
            activePeriod = mutableListOf(Alert.ActivePeriod(now.plus(20.minutes), null))
        }

    @Test
    fun `Leaf alertsDisplayOrder sorts and splits alerts in expected order no elevator `() =
        runBlocking {
            val displayAlerts =
                DisplayAlerts.forAlertsAtStop(
                    listOf(
                        hereMinorLater,
                        hereMajorLater,
                        hereElevatorLater,
                        hereElevatorNow,
                        hereMinorNow,
                        hereMajorNow,
                    ),
                    listOf(
                        downstreamMinorEvenLater,
                        downstreamMinorLater,
                        downstreamMajorLater,
                        downstreamMinorNow,
                        downstreamMajorNow,
                    ),
                    false,
                    now,
                )

            assertEquals(
                listOf(hereMajorNow, hereMinorNow),
                displayAlerts.highPriority.map { it.alert },
            )
            assertEquals(
                listOf(
                    downstreamMajorNow,
                    downstreamMinorNow,
                    hereMajorLater,
                    hereMinorLater,
                    downstreamMajorLater,
                    downstreamMinorLater,
                    downstreamMinorEvenLater,
                ),
                displayAlerts.lowPriority.map { it.alert },
            )
        }

    @Test
    fun `Leaf alertsDisplayOrder sorts and splits alerts in expected order with elevator `() =
        runBlocking {
            val displayAlerts =
                DisplayAlerts.forAlertsAtStop(
                    listOf(
                        hereMinorLater,
                        hereMajorLater,
                        hereElevatorLater,
                        hereElevatorNow,
                        hereMinorNow,
                        hereMajorNow,
                    ),
                    listOf(
                        downstreamMinorEvenLater,
                        downstreamMinorLater,
                        downstreamMajorLater,
                        downstreamMinorNow,
                        downstreamMajorNow,
                    ),
                    true,
                    now,
                )

            assertEquals(
                listOf(hereMajorNow, hereElevatorNow, hereMinorNow),
                displayAlerts.highPriority.map { it.alert },
            )
            assertEquals(
                listOf(
                    Pair(downstreamMajorNow, true),
                    Pair(downstreamMinorNow, true),
                    Pair(hereMajorLater, false),
                    Pair(hereElevatorLater, false),
                    Pair(hereMinorLater, false),
                    Pair(downstreamMajorLater, true),
                    Pair(downstreamMinorLater, true),
                    Pair(downstreamMinorEvenLater, true),
                ),
                displayAlerts.lowPriority.map { Pair(it.alert, it.isDownstream) },
            )
        }
}
