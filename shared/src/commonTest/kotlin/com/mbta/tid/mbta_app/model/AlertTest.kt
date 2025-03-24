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
    fun `alert significance is set properly`() {
        for (effect in Alert.Effect.entries) {
            val inherentlyStopSpecific = when (effect) {
                Alert.Effect.DockClosure, Alert.Effect.DockIssue -> true
                Alert.Effect.StationClosure, Alert.Effect.StationIssue -> true
                Alert.Effect.StopClosure -> true
                else -> false
            }
            for (specifiedStops in listOfNotNull(false.takeUnless { inherentlyStopSpecific }, true)) {
                val alert = ObjectCollectionBuilder.Single.alert {
                    this.effect = effect
                    informedEntity(emptyList(), stop = "stop".takeIf { specifiedStops })
                }
                val expectedSignificance = when (effect) {
                    Alert.Effect.Detour, Alert.Effect.SnowRoute -> if (specifiedStops) AlertSignificance.Major else AlertSignificance.Secondary
                    Alert.Effect.DockClosure -> AlertSignificance.Major
                    Alert.Effect.ElevatorClosure -> AlertSignificance.Accessibility
                    Alert.Effect.ServiceChange -> AlertSignificance.Secondary
                    Alert.Effect.Shuttle -> AlertSignificance.Major
                    Alert.Effect.StationClosure -> AlertSignificance.Major
                    Alert.Effect.StopClosure -> AlertSignificance.Major
                    Alert.Effect.Suspension -> AlertSignificance.Major
                    Alert.Effect.TrackChange -> AlertSignificance.Minor
                    else -> AlertSignificance.None
                }
                assertEquals(
                    expectedSignificance,
                    alert.significance,
                    "significance for effect $effect with${if (specifiedStops) "" else "out"} specified stops"
                )
            }
        }
    }

    @Test
    fun `alert significance for delay alerts`() {
                val subwayDelaySevere = ObjectCollectionBuilder.Single.alert {
                    effect = Alert.Effect.Delay
                    severity = 10
                    informedEntity(emptyList(), stop = "stop", routeType = RouteType.LIGHT_RAIL)
                }

        val subwayDelayNotSevere = ObjectCollectionBuilder.Single.alert {
            this.effect = Alert.Effect.Delay
            severity = 0
            informedEntity(emptyList(), stop = "stop", routeType = RouteType.LIGHT_RAIL)
        }

        val busDelaySevere = ObjectCollectionBuilder.Single.alert {
            this.effect = Alert.Effect.Delay
            severity = 10
            informedEntity(emptyList(), stop = "stop", routeType = RouteType.BUS)
        }

        assertEquals(subwayDelaySevere.significance, AlertSignificance.Minor)
        assertEquals(subwayDelayNotSevere.significance, AlertSignificance.None)
        assertEquals(busDelaySevere.significance, AlertSignificance.None)
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
            Alert.applicableAlerts(
                listOf(
                    alertMatch,
                    alertMatchNoDirection,
                    alertDifferentRoute,
                    alertDifferentStop,
                    alertDifferentDirection
                ),
                0,
                listOf(route.id),
                setOf(stop.id),
                tripId = null
            )

        assertEquals(listOf(alertMatch, alertMatchNoDirection), filteredList)
    }


    @Test
    fun `filters applicable alerts`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route { sortOrder = 1 }

        val validAlert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride
                    ),
                    route = route.id,
                    routeType = route.type,
                    stop = stop.id
                )
            }
        val invalidAlert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Exit, Alert.InformedEntity.Activity.Ride),
                    route = "wrong",
                    routeType = route.type,
                    stop = "wrong"
                )
            }
        assertEquals(
            Alert.applicableAlerts(
                listOf(validAlert, invalidAlert),
                null,
                listOf(route.id),
                setOf(stop.id),
                tripId = null,
            ),
            listOf(validAlert)
        )
    }

    @Test
    fun `filters out alerts without Board activity`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route { sortOrder = 1 }

        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Exit, Alert.InformedEntity.Activity.Ride),
                    route = route.id,
                    routeType = route.type,
                    stop = stop.id
                )
            }
        assertEquals(
            Alert.applicableAlerts(
                listOf(alert),
                null,
                        listOf(route.id),
                setOf(stop.id),
                tripId = null,
            ),
            emptyList()
        )
    }

    @Test
    fun `filters out alerts with non-matching route ID`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route { sortOrder = 1 }

        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Exit, Alert.InformedEntity.Activity.Ride),
                    route = "not matching",
                    routeType = route.type,
                    stop = stop.id
                )
            }
        assertEquals(
            Alert.applicableAlerts(
                listOf(alert),
                null,
                listOf(route.id),
                setOf(stop.id),
                tripId = null,
            ),
            emptyList()
        )
    }

    @Test
    fun `filters out alerts with non-matching stop ID`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route { sortOrder = 1 }

        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Exit, Alert.InformedEntity.Activity.Ride),
                    route = route.id,
                    routeType = route.type,
                    stop = "not matching"
                )
            }
        assertEquals(
            Alert.applicableAlerts(
                listOf(alert),
                null,
                listOf(route.id),
                setOf(stop.id),
                tripId = null,
            ),
            emptyList()
        )
    }

    @Test
    fun `filters to route only when not given set of stops`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { sortOrder = 1 }
        val otherRoute = objects.route { sortOrder = 1 }

        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = route.id,
                    routeType = route.type,
                    stop = "not matching"
                )
            }

        val otherAlert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = otherRoute.id,
                    routeType = otherRoute.type,
                    stop = "not matching"
                )
            }
        assertEquals(
            listOf(alert),
            Alert.applicableAlerts(   listOf(alert, otherAlert),
                directionId = null,
                listOf(route.id),
                stopIds = null,
                tripId = null
            ),
        )
    }

    @Test
    fun `downstreamAlerts returns alerts for first downstream alerting stop`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val targetStop = objects.stop()
        val stopWithBoardAlert = objects.stop()
        val firstStopWithRideAlert = objects.stop()
        val secondStopWithRideAlert = objects.stop()

        val alertRideTargetStop =
            objects.alert {
                effect = Alert.Effect.ServiceChange
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Ride),
                    route = route.id,
                    stop = targetStop.id,
                    directionId = 0
                )
            }

        val alertBoard =
            objects.alert {
                effect = Alert.Effect.ServiceChange
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = route.id,
                    stop = stopWithBoardAlert.id,
                    directionId = 0
                )
            }
        val firstRideAlert =
            objects.alert {
                effect = Alert.Effect.ServiceChange
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Ride),
                    route = route.id,
                    stop = firstStopWithRideAlert.id,
                    directionId = null
                )
            }

        val secondRideAlert =
            objects.alert {
                effect = Alert.Effect.ServiceChange
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Ride),
                    route = route.id,
                    stop = secondStopWithRideAlert.id,
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
                        stopWithBoardAlert.id,
                        firstStopWithRideAlert.id,
                        secondStopWithRideAlert.id
                    )
            }

        val downstreamAlerts =
            Alert.downstreamAlerts(
                listOf(alertRideTargetStop, alertBoard, firstRideAlert, secondRideAlert),
                trip,
                setOf(targetStop.id)
            )

        assertEquals(listOf(firstRideAlert), downstreamAlerts)
    }


    @Test
    fun `downstreamAlerts excludes alerts affecting the target stop`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val targetStop = objects.stop()
        val downstreamStop1 = objects.stop()
        val downstreamStop2 = objects.stop()

        val alertAllStops =
            objects.alert {
                effect = Alert.Effect.ServiceChange
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board, Alert.InformedEntity.Activity.Ride),
                    route = route.id,
                    stop = targetStop.id,
                    directionId = 0
                )
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board, Alert.InformedEntity.Activity.Ride),
                    route = route.id,
                    stop = downstreamStop1.id,
                    directionId = 0
                )
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board, Alert.InformedEntity.Activity.Ride),
                    route = route.id,
                    stop = downstreamStop2.id,
                    directionId = 0
                )
            }

        val alertDownstream2Only =
            objects.alert {
                effect = Alert.Effect.ServiceChange
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Ride),
                    route = route.id,
                    stop = downstreamStop2.id,
                    directionId = 0
                )
            }

        val trip =
            objects.trip {
                routeId = route.id
                directionId = 0
                stopIds =
                    listOf(
                        targetStop.id,
                        downstreamStop1.id,
                        downstreamStop2.id,
                    )
            }

        val downstreamAlerts =
            Alert.downstreamAlerts(
                listOf(alertAllStops, alertDownstream2Only),
                trip,
                setOf(targetStop.id)
            )

        assertEquals(listOf(alertDownstream2Only), downstreamAlerts)
    }



    @Test
    fun `downstreamAlerts ignores alert without stops specified`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val targetStop = objects.stop()
        val nextStop = objects.stop()

        val alert =
            objects.alert {
                effect = Alert.Effect.ServiceChange
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = route.id,
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
                        nextStop.id,
                    )
            }

        val downstreamAlerts =
            Alert.downstreamAlerts(
                listOf(alert),
                trip,
                setOf(targetStop.id)
            )

        assertEquals(listOf(), downstreamAlerts)
    }

    @Test
    fun `elevatorAlerts matches relevant accessibility alerts`() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()

        val elevatorAlert =
            objects.alert {
                effect = Alert.Effect.ElevatorClosure
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.UsingWheelchair),
                    stop = stop.id
                )
            }
        val serviceAlert =
            objects.alert {
                effect = Alert.Effect.NoService
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    stop = stop.id
                )
            }

        val alerts =
            Alert.elevatorAlerts(
                listOf(serviceAlert, elevatorAlert),
                setOf(stop.id)
            )

        assertEquals(listOf(elevatorAlert), alerts)
    }
}
