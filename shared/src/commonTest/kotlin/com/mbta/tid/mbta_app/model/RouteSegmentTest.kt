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
                sourceRouteId = "sourceRoute",
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("place-davis"),
                otherPatternsByStopId = mapOf()
            )

        assertEquals(mapOf(), segment.alertStateByStopId(mapOf()))
    }

    @Test
    fun `alertStateByStopId excludes stop when alerts for stop are not service alerts`() {
        val segment =
            RouteSegment(
                id = "id",
                sourceRouteId = "sourceRoute",
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("place-davis"),
                otherPatternsByStopId = mapOf()
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
                                    Alert.InformedEntity.Activity.Ride
                                ),
                                route = "sourceRoute",
                                routeType = RouteType.HEAVY_RAIL,
                                stop = "place-davis"
                            )
                        }
                    ),
                serviceStatus = StopServiceStatus.NORMAL
            )

        assertEquals(mapOf(), segment.alertStateByStopId(mapOf("place-davis" to alertsForStop)))
    }

    @Test
    fun `alertStateByStopId includes stop when at least one service alert`() {
        val segment =
            RouteSegment(
                id = "id",
                sourceRouteId = "sourceRoute",
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("place-davis"),
                otherPatternsByStopId = mapOf()
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
                                    Alert.InformedEntity.Activity.Ride
                                ),
                                route = "sourceRoute",
                                routeType = RouteType.HEAVY_RAIL,
                                stop = "place-davis"
                            )
                        },
                        alert {
                            effect = Alert.Effect.Delay
                            informedEntity(
                                listOf(
                                    Alert.InformedEntity.Activity.Board,
                                    Alert.InformedEntity.Activity.Exit,
                                    Alert.InformedEntity.Activity.Ride
                                ),
                                route = "sourceRoute",
                                routeType = RouteType.HEAVY_RAIL,
                                stop = "place-davis"
                            )
                        }
                    ),
                serviceStatus = StopServiceStatus.NORMAL
            )

        assertEquals(
            mapOf("place-davis" to RouteSegment.StopAlertState(hasAlert = true)),
            segment.alertStateByStopId(mapOf("place-davis" to alertsForStop))
        )
    }

    @Test
    fun `alertStateByStopId excludes stop when service alert is not for the segment's route`() {
        val segment =
            RouteSegment(
                id = "id",
                sourceRouteId = "sourceRoute",
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("place-davis"),
                otherPatternsByStopId = mapOf()
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
                                    Alert.InformedEntity.Activity.Ride
                                ),
                                route = "otherRoute",
                                routeType = RouteType.HEAVY_RAIL,
                                stop = "place-davis"
                            )
                        }
                    ),
                serviceStatus = StopServiceStatus.NORMAL
            )

        assertEquals(mapOf(), segment.alertStateByStopId(mapOf("place-davis" to alertsForStop)))
    }

    @Test
    fun `alertStateByStopId has stop when service alert is for included route of segment`() {
        val segment =
            RouteSegment(
                id = "id",
                sourceRouteId = "sourceRoute",
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("place-davis"),
                otherPatternsByStopId =
                    mapOf(
                        "place-davis" to listOf(RoutePatternKey("otherRoute", "otherRoutePattern"))
                    )
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
                                    Alert.InformedEntity.Activity.Ride
                                ),
                                route = "sourceRoute",
                                routeType = RouteType.HEAVY_RAIL,
                                stop = "place-davis"
                            )
                        }
                    ),
                serviceStatus = StopServiceStatus.NORMAL
            )

        assertEquals(
            mapOf("place-davis" to RouteSegment.StopAlertState(hasAlert = true)),
            segment.alertStateByStopId(mapOf("place-davis" to alertsForStop))
        )
    }

    @Test
    fun `alertingSegments when alerting segment in the middle splits so alert in each segment`() {

        assertEquals(
            listOf(
                Pair(SegmentAlertState.Normal, listOf("alewife", "davis", "porter")),
                Pair(SegmentAlertState.Alert, listOf("porter", "harvard")),
                Pair(SegmentAlertState.Normal, listOf("harvard", "central"))
            ),
            RouteSegment.alertingSegments(
                listOf("alewife", "davis", "porter", "harvard", "central"),
                mapOf(
                    "porter" to RouteSegment.StopAlertState(hasAlert = true),
                    "harvard" to RouteSegment.StopAlertState(hasAlert = true)
                )
            )
        )
    }

    @Test
    fun `alertingSegments ignores alert that only touches terminal`() {

        assertEquals(
            listOf(
                Pair(SegmentAlertState.Alert, listOf("alewife", "davis")),
                Pair(SegmentAlertState.Normal, listOf("davis", "porter", "harvard", "central"))
            ),
            RouteSegment.alertingSegments(
                listOf("alewife", "davis", "porter", "harvard", "central"),
                mapOf(
                    "alewife" to RouteSegment.StopAlertState(hasAlert = true),
                    "davis" to RouteSegment.StopAlertState(hasAlert = true),
                    "central" to RouteSegment.StopAlertState(hasAlert = true)
                )
            )
        )
    }

    @Test
    fun `alertingSegments when all alerting returns one segment`() {

        assertEquals(
            listOf(
                Pair(
                    SegmentAlertState.Alert,
                    listOf("alewife", "davis", "porter", "harvard", "central")
                ),
            ),
            RouteSegment.alertingSegments(
                listOf("alewife", "davis", "porter", "harvard", "central"),
                mapOf(
                    "alewife" to RouteSegment.StopAlertState(hasAlert = true),
                    "davis" to RouteSegment.StopAlertState(hasAlert = true),
                    "porter" to RouteSegment.StopAlertState(hasAlert = true),
                    "harvard" to RouteSegment.StopAlertState(hasAlert = true),
                    "central" to RouteSegment.StopAlertState(hasAlert = true)
                )
            )
        )
    }

    @Test
    fun `alertingSegments when none alerting returns one segment`() {

        assertEquals(
            listOf(
                Pair(
                    SegmentAlertState.Normal,
                    listOf("alewife", "davis", "porter", "harvard", "central")
                ),
            ),
            RouteSegment.alertingSegments(
                listOf("alewife", "davis", "porter", "harvard", "central"),
                mapOf()
            )
        )
    }

    @Test
    fun `splitAlertingSegments when alerting segment in the middle splits so alert in each segment`() {
        var routeSegment =
            RouteSegment(
                id = "id",
                sourceRouteId = "sourceRoute",
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("alewife", "davis", "porter", "harvard", "central"),
                otherPatternsByStopId =
                    mapOf(
                        "alewife" to
                            listOf(
                                RoutePatternKey(routeId = "otherRoute", routePatternId = "otherRp")
                            )
                    )
            )

        val alertsForStop =
            mapOf(
                "davis" to serviceAlert("davis", routeSegment.sourceRouteId),
                "porter" to serviceAlert("porter", routeSegment.sourceRouteId)
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
                                        routeId = "otherRoute",
                                        routePatternId = "otherRp"
                                    )
                                )
                        ),
                    alertState = SegmentAlertState.Normal
                ),
                AlertAwareRouteSegment(
                    id = "id-1",
                    sourceRoutePatternId = routeSegment.sourceRoutePatternId,
                    sourceRouteId = routeSegment.sourceRouteId,
                    stopIds = listOf("davis", "porter"),
                    otherPatternsByStopId = mapOf(),
                    alertState = SegmentAlertState.Alert
                ),
                AlertAwareRouteSegment(
                    id = "id-2",
                    sourceRoutePatternId = routeSegment.sourceRoutePatternId,
                    sourceRouteId = routeSegment.sourceRouteId,
                    stopIds = listOf("porter", "harvard", "central"),
                    otherPatternsByStopId = mapOf(),
                    alertState = SegmentAlertState.Normal
                ),
            ),
            routeSegment.splitAlertingSegments(alertsForStop)
        )
    }

    @Test
    fun `splitAlertingSegments when all alerting returns one segment`() {
        var routeSegment =
            RouteSegment(
                id = "id",
                sourceRouteId = "sourceRoute",
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("alewife", "davis", "porter"),
                otherPatternsByStopId =
                    mapOf(
                        "alewife" to
                            listOf(
                                RoutePatternKey(routeId = "otherRoute", routePatternId = "otherRp")
                            )
                    )
            )

        val alertsForStop =
            mapOf(
                "alewife" to serviceAlert("alewife", routeSegment.sourceRouteId),
                "davis" to serviceAlert("davis", routeSegment.sourceRouteId),
                "porter" to serviceAlert("porter", routeSegment.sourceRouteId)
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
                                        routeId = "otherRoute",
                                        routePatternId = "otherRp"
                                    )
                                )
                        ),
                    alertState = SegmentAlertState.Alert
                ),
            ),
            routeSegment.splitAlertingSegments(alertsForStop)
        )
    }

    @Test
    fun `splitAlertingSegments when none alerting returns one segment`() {

        var routeSegment =
            RouteSegment(
                id = "id",
                sourceRouteId = "sourceRoute",
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("alewife", "davis", "porter"),
                otherPatternsByStopId =
                    mapOf(
                        "alewife" to
                            listOf(
                                RoutePatternKey(routeId = "otherRoute", routePatternId = "otherRp")
                            )
                    )
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
                                        routeId = "otherRoute",
                                        routePatternId = "otherRp"
                                    )
                                )
                        ),
                    alertState = SegmentAlertState.Normal
                ),
            ),
            routeSegment.splitAlertingSegments(alertsForStop)
        )
    }

    private fun serviceAlert(stopId: String, routeId: String): AlertAssociatedStop {
        return AlertAssociatedStop(
            stop = stop { id = stopId },
            relevantAlerts =
                listOf(
                    alert {
                        effect = Alert.Effect.Shuttle
                        informedEntity(
                            listOf(
                                Alert.InformedEntity.Activity.Board,
                                Alert.InformedEntity.Activity.Exit,
                                Alert.InformedEntity.Activity.Ride
                            ),
                            route = routeId,
                            routeType = RouteType.HEAVY_RAIL,
                            stop = stopId
                        )
                    },
                ),
            serviceStatus = StopServiceStatus.NORMAL
        )
    }
}
