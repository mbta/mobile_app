package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.stop
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteSegmentTest {

    @Test
    fun `alertStateByStopId excludes stops without alerts`() {

        val segment =
            RouteSegment(
                id = "id",
                sourceRouteId = Route.Id("sourceRoute"),
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("place-davis"),
                otherPatternsByStopId = mapOf(),
            )

        assertEquals(mapOf(), segment.alertStateByStopId(mapOf()))
    }

    @Test
    fun `alertStateByStopId excludes stop when alerts for stop are not service alerts`() {
        val segment =
            RouteSegment(
                id = "id",
                sourceRouteId = Route.Id("sourceRoute"),
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("place-davis"),
                otherPatternsByStopId = mapOf(),
            )

        val alertsForStop =
            AlertAssociatedStop(
                stop = stop { id = "place-davis" },
                relevantAlerts =
                    listOf(
                        alert {
                            effect = Alert.Effect.Delay
                            informedEntity(
                                listOf(
                                    Alert.InformedEntity.Activity.Board,
                                    Alert.InformedEntity.Activity.Exit,
                                    Alert.InformedEntity.Activity.Ride,
                                ),
                                route = "sourceRoute",
                                routeType = RouteType.HEAVY_RAIL,
                                stop = "place-davis",
                            )
                        }
                    ),
                stateByRoute = mapOf(Pair(MapStopRoute.RED, StopAlertState.Normal)),
            )

        assertEquals(mapOf(), segment.alertStateByStopId(mapOf("place-davis" to alertsForStop)))
    }

    @Test
    fun `alertStateByStopId includes stop when at least one service alert`() {
        val segment =
            RouteSegment(
                id = "id",
                sourceRouteId = Route.Id("sourceRoute"),
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("place-davis"),
                otherPatternsByStopId = mapOf(),
            )

        val alertsForStop =
            AlertAssociatedStop(
                stop = stop { id = "place-davis" },
                relevantAlerts =
                    listOf(
                        alert {
                            effect = Alert.Effect.Shuttle
                            informedEntity(
                                listOf(
                                    Alert.InformedEntity.Activity.Board,
                                    Alert.InformedEntity.Activity.Exit,
                                    Alert.InformedEntity.Activity.Ride,
                                ),
                                route = "sourceRoute",
                                routeType = RouteType.HEAVY_RAIL,
                                stop = "place-davis",
                            )
                        },
                        alert {
                            effect = Alert.Effect.Delay
                            informedEntity(
                                listOf(
                                    Alert.InformedEntity.Activity.Board,
                                    Alert.InformedEntity.Activity.Exit,
                                    Alert.InformedEntity.Activity.Ride,
                                ),
                                route = "sourceRoute",
                                routeType = RouteType.HEAVY_RAIL,
                                stop = "place-davis",
                            )
                        },
                    ),
                stateByRoute = mapOf(Pair(MapStopRoute.RED, StopAlertState.Normal)),
            )

        assertEquals(
            mapOf(
                "place-davis" to
                    RouteSegment.StopAlertState(hasSuspension = false, hasShuttle = true)
            ),
            segment.alertStateByStopId(mapOf("place-davis" to alertsForStop)),
        )
    }

    @Test
    fun `alertStateByStopId handles full spectrum of possible states`() {
        val segment =
            RouteSegment(
                id = "id",
                sourceRouteId = Route.Id("sourceRoute"),
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds =
                    listOf("place-neither", "place-shuttle", "place-suspension", "place-both"),
                otherPatternsByStopId = emptyMap(),
            )

        val alerts =
            mapOf(
                "place-shuttle" to
                    serviceAlert("place-shuttle", "sourceRoute", Alert.Effect.Shuttle),
                "place-suspension" to
                    serviceAlert("place-suspension", "sourceRoute", Alert.Effect.Suspension),
                "place-both" to
                    AlertAssociatedStop(
                        stop = stop { id = "place-both" },
                        relevantAlerts =
                            listOf(
                                alert {
                                    effect = Alert.Effect.Shuttle
                                    informedEntity(
                                        listOf(
                                            Alert.InformedEntity.Activity.Board,
                                            Alert.InformedEntity.Activity.Exit,
                                            Alert.InformedEntity.Activity.Ride,
                                        ),
                                        route = "sourceRoute",
                                        routeType = RouteType.HEAVY_RAIL,
                                        stop = "place-both",
                                    )
                                },
                                alert {
                                    effect = Alert.Effect.Suspension
                                    informedEntity(
                                        listOf(
                                            Alert.InformedEntity.Activity.Board,
                                            Alert.InformedEntity.Activity.Exit,
                                            Alert.InformedEntity.Activity.Ride,
                                        ),
                                        route = "sourceRoute",
                                        routeType = RouteType.HEAVY_RAIL,
                                        stop = "place-both",
                                    )
                                },
                            ),
                        stateByRoute = mapOf(Pair(MapStopRoute.RED, StopAlertState.Issue)),
                    ),
            )

        assertEquals(
            mapOf(
                "place-shuttle" to
                    RouteSegment.StopAlertState(hasSuspension = false, hasShuttle = true),
                "place-suspension" to
                    RouteSegment.StopAlertState(hasSuspension = true, hasShuttle = false),
                "place-both" to RouteSegment.StopAlertState(hasSuspension = true, hasShuttle = true),
            ),
            segment.alertStateByStopId(alerts),
        )
    }

    @Test
    fun `alertStateByStopId excludes stop when service alert is not for the segment's route`() {
        val segment =
            RouteSegment(
                id = "id",
                sourceRouteId = Route.Id("sourceRoute"),
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("place-davis"),
                otherPatternsByStopId = mapOf(),
            )

        val alertsForStop =
            AlertAssociatedStop(
                stop = stop { id = "place-davis" },
                relevantAlerts =
                    listOf(
                        alert {
                            effect = Alert.Effect.Shuttle
                            informedEntity(
                                listOf(
                                    Alert.InformedEntity.Activity.Board,
                                    Alert.InformedEntity.Activity.Exit,
                                    Alert.InformedEntity.Activity.Ride,
                                ),
                                route = "otherRoute",
                                routeType = RouteType.HEAVY_RAIL,
                                stop = "place-davis",
                            )
                        }
                    ),
                stateByRoute = mapOf(Pair(MapStopRoute.RED, StopAlertState.Normal)),
            )

        assertEquals(mapOf(), segment.alertStateByStopId(mapOf("place-davis" to alertsForStop)))
    }

    @Test
    fun `alertStateByStopId has stop when service alert is for included route of segment`() {
        val segment =
            RouteSegment(
                id = "id",
                sourceRouteId = Route.Id("sourceRoute"),
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("place-davis"),
                otherPatternsByStopId =
                    mapOf(
                        "place-davis" to
                            listOf(RoutePatternKey(Route.Id("otherRoute"), "otherRoutePattern"))
                    ),
            )

        val alertsForStop =
            AlertAssociatedStop(
                stop = stop { id = "place-davis" },
                relevantAlerts =
                    listOf(
                        alert {
                            effect = Alert.Effect.Shuttle
                            informedEntity(
                                listOf(
                                    Alert.InformedEntity.Activity.Board,
                                    Alert.InformedEntity.Activity.Exit,
                                    Alert.InformedEntity.Activity.Ride,
                                ),
                                route = "sourceRoute",
                                routeType = RouteType.HEAVY_RAIL,
                                stop = "place-davis",
                            )
                        }
                    ),
                stateByRoute = mapOf(Pair(MapStopRoute.RED, StopAlertState.Normal)),
            )

        assertEquals(
            mapOf(
                "place-davis" to
                    RouteSegment.StopAlertState(hasSuspension = false, hasShuttle = true)
            ),
            segment.alertStateByStopId(mapOf("place-davis" to alertsForStop)),
        )
    }

    @Test
    fun `alertStateByStopId includes stop when only children have service alerts`() {
        lateinit var child1: Stop
        lateinit var child2: Stop
        val stop = stop {
            id = "place-butlr"
            child1 = childStop()
            child2 = childStop()
        }

        val alert = alert {
            effect = Alert.Effect.Shuttle
            informedEntity(
                listOf(
                    Alert.InformedEntity.Activity.Board,
                    Alert.InformedEntity.Activity.Exit,
                    Alert.InformedEntity.Activity.Ride,
                ),
                routeType = RouteType.LIGHT_RAIL,
                route = "Mattapan",
            )
        }

        val segment =
            RouteSegment(
                id = "id",
                sourceRouteId = Route.Id("Mattapan"),
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("place-butlr"),
                otherPatternsByStopId = mapOf(),
            )

        val alertsForStop =
            AlertAssociatedStop(
                stop = stop,
                relevantAlerts = emptyList(),
                childAlerts =
                    mapOf(
                        child1.id to
                            AlertAssociatedStop(
                                stop = child1,
                                relevantAlerts = listOf(alert),
                                stateByRoute =
                                    mapOf(MapStopRoute.MATTAPAN to StopAlertState.Shuttle),
                            ),
                        child2.id to
                            AlertAssociatedStop(
                                stop = child2,
                                relevantAlerts = listOf(alert),
                                stateByRoute =
                                    mapOf(MapStopRoute.MATTAPAN to StopAlertState.Shuttle),
                            ),
                    ),
                stateByRoute = mapOf(MapStopRoute.MATTAPAN to StopAlertState.Shuttle),
            )

        assertEquals(
            mapOf(
                "place-butlr" to
                    RouteSegment.StopAlertState(hasSuspension = false, hasShuttle = true)
            ),
            segment.alertStateByStopId(mapOf("place-butlr" to alertsForStop)),
        )
    }

    @Test
    fun `alertingSegments when alerting segment in the middle splits so alert in each segment`() {

        assertEquals(
            listOf(
                Pair(SegmentAlertState.Normal, listOf("alewife", "davis", "porter")),
                Pair(SegmentAlertState.Suspension, listOf("porter", "harvard")),
                Pair(SegmentAlertState.Normal, listOf("harvard", "central")),
            ),
            RouteSegment.alertingSegments(
                listOf("alewife", "davis", "porter", "harvard", "central"),
                mapOf(
                    "porter" to
                        RouteSegment.StopAlertState(hasSuspension = true, hasShuttle = false),
                    "harvard" to
                        RouteSegment.StopAlertState(hasSuspension = true, hasShuttle = false),
                ),
            ),
        )
    }

    @Test
    fun `alertingSegments ignores alert that only touches terminal`() {

        assertEquals(
            listOf(
                Pair(SegmentAlertState.Suspension, listOf("alewife", "davis")),
                Pair(SegmentAlertState.Normal, listOf("davis", "porter", "harvard", "central")),
            ),
            RouteSegment.alertingSegments(
                listOf("alewife", "davis", "porter", "harvard", "central"),
                mapOf(
                    "alewife" to
                        RouteSegment.StopAlertState(hasSuspension = true, hasShuttle = false),
                    "davis" to
                        RouteSegment.StopAlertState(hasSuspension = true, hasShuttle = false),
                    "central" to
                        RouteSegment.StopAlertState(hasSuspension = true, hasShuttle = false),
                ),
            ),
        )
    }

    @Test
    fun `alertingSegments when all alerting returns one segment`() {

        assertEquals(
            listOf(
                Pair(
                    SegmentAlertState.Suspension,
                    listOf("alewife", "davis", "porter", "harvard", "central"),
                )
            ),
            RouteSegment.alertingSegments(
                listOf("alewife", "davis", "porter", "harvard", "central"),
                mapOf(
                    "alewife" to
                        RouteSegment.StopAlertState(hasSuspension = true, hasShuttle = false),
                    "davis" to
                        RouteSegment.StopAlertState(hasSuspension = true, hasShuttle = false),
                    "porter" to
                        RouteSegment.StopAlertState(hasSuspension = true, hasShuttle = false),
                    "harvard" to
                        RouteSegment.StopAlertState(hasSuspension = true, hasShuttle = false),
                    "central" to
                        RouteSegment.StopAlertState(hasSuspension = true, hasShuttle = false),
                ),
            ),
        )
    }

    @Test
    fun `alertingSegments when none alerting returns one segment`() {

        assertEquals(
            listOf(
                Pair(
                    SegmentAlertState.Normal,
                    listOf("alewife", "davis", "porter", "harvard", "central"),
                )
            ),
            RouteSegment.alertingSegments(
                listOf("alewife", "davis", "porter", "harvard", "central"),
                mapOf(),
            ),
        )
    }

    @Test
    fun `alertingSegments handles transition from suspension to shuttle`() {
        assertEquals(
            listOf(
                Pair(SegmentAlertState.Normal, listOf("a", "b", "c")),
                Pair(SegmentAlertState.Suspension, listOf("c", "d", "e")),
                Pair(SegmentAlertState.Shuttle, listOf("e", "f", "g")),
                Pair(SegmentAlertState.Normal, listOf("g", "h", "i")),
            ),
            RouteSegment.alertingSegments(
                listOf("a", "b", "c", "d", "e", "f", "g", "h", "i"),
                mapOf(
                    "c" to RouteSegment.StopAlertState(hasSuspension = true, hasShuttle = false),
                    "d" to RouteSegment.StopAlertState(hasSuspension = true, hasShuttle = false),
                    "e" to RouteSegment.StopAlertState(hasSuspension = true, hasShuttle = true),
                    "f" to RouteSegment.StopAlertState(hasSuspension = false, hasShuttle = true),
                    "g" to RouteSegment.StopAlertState(hasSuspension = false, hasShuttle = true),
                ),
            ),
        )
    }

    @Test
    fun `splitAlertingSegments when alerting segment in the middle splits so alert in each segment`() {
        var routeSegment =
            RouteSegment(
                id = "id",
                sourceRouteId = Route.Id("sourceRoute"),
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("alewife", "davis", "porter", "harvard", "central"),
                otherPatternsByStopId =
                    mapOf(
                        "alewife" to
                            listOf(
                                RoutePatternKey(
                                    routeId = Route.Id("otherRoute"),
                                    routePatternId = "otherRp",
                                )
                            )
                    ),
            )

        val alertsForStop =
            mapOf(
                "davis" to
                    serviceAlert("davis", routeSegment.sourceRouteId, Alert.Effect.Suspension),
                "porter" to
                    serviceAlert("porter", routeSegment.sourceRouteId, Alert.Effect.Suspension),
            )

        assertEquals(
            listOf(
                AlertAwareRouteSegment(
                    id = "id-0",
                    sourceRoutePatternId = routeSegment.sourceRoutePatternId,
                    sourceRouteId = routeSegment.sourceRouteId,
                    stopIds = listOf("alewife", "davis"),
                    otherPatternsByStopId =
                        mapOf(
                            "alewife" to
                                listOf(
                                    RoutePatternKey(
                                        routeId = Route.Id("otherRoute"),
                                        routePatternId = "otherRp",
                                    )
                                )
                        ),
                    alertState = SegmentAlertState.Normal,
                ),
                AlertAwareRouteSegment(
                    id = "id-1",
                    sourceRoutePatternId = routeSegment.sourceRoutePatternId,
                    sourceRouteId = routeSegment.sourceRouteId,
                    stopIds = listOf("davis", "porter"),
                    otherPatternsByStopId = mapOf(),
                    alertState = SegmentAlertState.Suspension,
                ),
                AlertAwareRouteSegment(
                    id = "id-2",
                    sourceRoutePatternId = routeSegment.sourceRoutePatternId,
                    sourceRouteId = routeSegment.sourceRouteId,
                    stopIds = listOf("porter", "harvard", "central"),
                    otherPatternsByStopId = mapOf(),
                    alertState = SegmentAlertState.Normal,
                ),
            ),
            routeSegment.splitAlertingSegments(alertsForStop),
        )
    }

    @Test
    fun `splitAlertingSegments when all alerting returns one segment`() {
        var routeSegment =
            RouteSegment(
                id = "id",
                sourceRouteId = Route.Id("sourceRoute"),
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("alewife", "davis", "porter"),
                otherPatternsByStopId =
                    mapOf(
                        "alewife" to
                            listOf(
                                RoutePatternKey(
                                    routeId = Route.Id("otherRoute"),
                                    routePatternId = "otherRp",
                                )
                            )
                    ),
            )

        val alertsForStop =
            mapOf(
                "alewife" to
                    serviceAlert("alewife", routeSegment.sourceRouteId, Alert.Effect.Suspension),
                "davis" to
                    serviceAlert("davis", routeSegment.sourceRouteId, Alert.Effect.Suspension),
                "porter" to
                    serviceAlert("porter", routeSegment.sourceRouteId, Alert.Effect.Suspension),
            )

        assertEquals(
            listOf(
                AlertAwareRouteSegment(
                    id = "id-0",
                    sourceRoutePatternId = routeSegment.sourceRoutePatternId,
                    sourceRouteId = routeSegment.sourceRouteId,
                    stopIds = listOf("alewife", "davis", "porter"),
                    otherPatternsByStopId =
                        mapOf(
                            "alewife" to
                                listOf(
                                    RoutePatternKey(
                                        routeId = Route.Id("otherRoute"),
                                        routePatternId = "otherRp",
                                    )
                                )
                        ),
                    alertState = SegmentAlertState.Suspension,
                )
            ),
            routeSegment.splitAlertingSegments(alertsForStop),
        )
    }

    @Test
    fun `splitAlertingSegments when none alerting returns one segment`() {

        var routeSegment =
            RouteSegment(
                id = "id",
                sourceRouteId = Route.Id("sourceRoute"),
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("alewife", "davis", "porter"),
                otherPatternsByStopId =
                    mapOf(
                        "alewife" to
                            listOf(
                                RoutePatternKey(
                                    routeId = Route.Id("otherRoute"),
                                    routePatternId = "otherRp",
                                )
                            )
                    ),
            )

        val alertsForStop: Map<String, AlertAssociatedStop> = mapOf()

        assertEquals(
            listOf(
                AlertAwareRouteSegment(
                    id = "id-0",
                    sourceRoutePatternId = routeSegment.sourceRoutePatternId,
                    sourceRouteId = routeSegment.sourceRouteId,
                    stopIds = listOf("alewife", "davis", "porter"),
                    otherPatternsByStopId =
                        mapOf(
                            "alewife" to
                                listOf(
                                    RoutePatternKey(
                                        routeId = Route.Id("otherRoute"),
                                        routePatternId = "otherRp",
                                    )
                                )
                        ),
                    alertState = SegmentAlertState.Normal,
                )
            ),
            routeSegment.splitAlertingSegments(alertsForStop),
        )
    }

    private fun serviceAlert(stopId: String, routeId: String, effect: Alert.Effect) =
        serviceAlert(stopId, Route.Id(routeId), effect)

    private fun serviceAlert(
        stopId: String,
        routeId: Route.Id,
        effect: Alert.Effect,
    ): AlertAssociatedStop {
        return AlertAssociatedStop(
            stop = stop { id = stopId },
            relevantAlerts =
                listOf(
                    alert {
                        this.effect = effect
                        informedEntity(
                            listOf(
                                Alert.InformedEntity.Activity.Board,
                                Alert.InformedEntity.Activity.Exit,
                                Alert.InformedEntity.Activity.Ride,
                            ),
                            route = routeId.idText,
                            routeType = RouteType.HEAVY_RAIL,
                            stop = stopId,
                        )
                    }
                ),
            stateByRoute = mapOf(Pair(MapStopRoute.RED, StopAlertState.Normal)),
        )
    }
}
