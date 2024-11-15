package com.mbta.tid.mbta_app.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AlertTest {
    @Test
    fun `alert status is set properly`() {
        val objects = ObjectCollectionBuilder()
        val closureAlert = objects.alert { effect = Alert.Effect.StationClosure }
        val shuttleAlert = objects.alert { effect = Alert.Effect.Shuttle }
        val detourAlert = objects.alert { effect = Alert.Effect.Detour }
        val issueAlert = objects.alert { effect = Alert.Effect.ParkingIssue }

        assertEquals(StopAlertState.Suspension, closureAlert.alertState)
        assertEquals(StopAlertState.Shuttle, shuttleAlert.alertState)
        assertEquals(StopAlertState.Issue, detourAlert.alertState)
        assertEquals(StopAlertState.Issue, issueAlert.alertState)
    }

    @Test
    fun `filter filters alerts to matching stops routes and directions`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val stop = objects.stop()

        val alertMatch =
            objects.alert {
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = route.id,
                    stop = stop.id,
                    directionId = 0
                )
            }
        val alertMatchNoDirection =
            objects.alert {
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = route.id,
                    stop = stop.id,
                    directionId = null
                )
            }
        val alertDifferentRoute =
            objects.alert {
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = "other_route",
                    stop = stop.id,
                    directionId = 0
                )
            }
        val alertDifferentStop =
            objects.alert {
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = route.id,
                    stop = "other_stop",
                    directionId = 0
                )
            }
        val alertDifferentDirection =
            objects.alert {
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = route.id,
                    stop = stop.id,
                    directionId = 1
                )
            }
        val filteredList =
            Alert.filter(
                listOf(
                    alertMatch,
                    alertMatchNoDirection,
                    alertDifferentRoute,
                    alertDifferentStop,
                    alertDifferentDirection
                ),
                0,
                listOf(route.id),
                setOf(stop.id)
            )

        assertEquals(listOf(alertMatch, alertMatchNoDirection), filteredList)
    }

    @Test
    fun `downstreamAlerts returns alerts for first downstream alerting stop`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val targetStop = objects.stop()
        val stopWithRideAlert = objects.stop()
        val firstStopWithBoardAlert = objects.stop()
        val secondStopWithBoardAlert = objects.stop()

        val alertBoardTargetStop =
            objects.alert {
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = route.id,
                    stop = targetStop.id,
                    directionId = 0
                )
            }

        val alertRide =
            objects.alert {
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Ride),
                    route = route.id,
                    stop = stopWithRideAlert.id,
                    directionId = 0
                )
            }
        val firstBoardAlert =
            objects.alert {
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = route.id,
                    stop = firstStopWithBoardAlert.id,
                    directionId = null
                )
            }

        val secondBoardAlert =
            objects.alert {
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = route.id,
                    stop = secondStopWithBoardAlert.id,
                    directionId = null
                )
            }

        val trip =
            objects.trip {
                routeId = route.id
                directionId = 0
                stopIds =
                    listOf(
                        targetStop.id,
                        stopWithRideAlert.id,
                        firstStopWithBoardAlert.id,
                        secondStopWithBoardAlert.id
                    )
            }

        val downstreamAlerts =
            Alert.downstreamAlerts(
                listOf(alertBoardTargetStop, alertRide, firstBoardAlert, secondBoardAlert),
                trip,
                setOf(targetStop.id)
            )

        assertEquals(listOf(firstBoardAlert), downstreamAlerts)
    }
}
