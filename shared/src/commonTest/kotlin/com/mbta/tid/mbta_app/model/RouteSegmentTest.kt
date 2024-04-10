package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.stop
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteSegmentTest {

    @Test
    fun `hasServiceAlertByStopId excludes stops without alerts`() {

        val segment =
            RouteSegment(
                id = "id",
                sourceRouteId = "sourceRoute",
                sourceRoutePatternId = "sourceRoutePattern",
                stopIds = listOf("place-davis"),
                otherPatternsByStopId = mapOf()
            )

        assertEquals(setOf(), segment.hasServiceAlertByStopId(mapOf()))
    }

    @Test
    fun `hasServiceAlertByStopId excludes stop when alerts for stop are not service alerts`() {
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

        assertEquals(
            setOf(),
            segment.hasServiceAlertByStopId(mapOf("place-davis" to alertsForStop))
        )
    }

    @Test
    fun `hasServiceAlertByStopId includes stop when at least one service alert`() {
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
            setOf("place-davis"),
            segment.hasServiceAlertByStopId(mapOf("place-davis" to alertsForStop))
        )
    }

    @Test
    fun `hasServiceAlertByStopId excludes stop when service alert is not for the segment's route`() {
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

        assertEquals(
            setOf(),
            segment.hasServiceAlertByStopId(mapOf("place-davis" to alertsForStop))
        )
    }

    @Test
    fun `hasServiceAlertByStopId has stop when service alert is for included route of segment`() {
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
            setOf("place-davis"),
            segment.hasServiceAlertByStopId(mapOf("place-davis" to alertsForStop))
        )
    }

    @Test
    fun `alertingSegments when alerting segment in the middle splits so alert in each segment`() {

        assertEquals(
            listOf(
                Pair(false, listOf("alewife", "davis", "porter")),
                Pair(true, listOf("porter", "harvard")),
                Pair(false, listOf("harvard", "central"))
            ),
            RouteSegment.alertingSegments(
                listOf("alewife", "davis", "porter", "harvard", "central"),
                setOf("porter", "harvard")
            )
        )
    }

    @Test
    fun `alertingSegments when alerting segment at the ends splits so alert in each segment`() {

        assertEquals(
            listOf(
                Pair(true, listOf("alewife", "davis")),
                Pair(false, listOf("davis", "porter", "harvard", "central")),
                Pair(true, listOf("central"))
            ),
            RouteSegment.alertingSegments(
                listOf("alewife", "davis", "porter", "harvard", "central"),
                setOf("alewife", "davis", "central")
            )
        )
    }

    @Test
    fun `alertingSegments when all alerting returns one segment`() {

        assertEquals(
            listOf(
                Pair(true, listOf("alewife", "davis", "porter", "harvard", "central")),
            ),
            RouteSegment.alertingSegments(
                listOf("alewife", "davis", "porter", "harvard", "central"),
                setOf("alewife", "davis", "porter", "harvard", "central")
            )
        )
    }

    @Test
    fun `alertingSegments when none alerting returns one segment`() {

        assertEquals(
            listOf(
                Pair(false, listOf("alewife", "davis", "porter", "harvard", "central")),
            ),
            RouteSegment.alertingSegments(
                listOf("alewife", "davis", "porter", "harvard", "central"),
                setOf()
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
                    isAlerting = false
                ),
                AlertAwareRouteSegment(
                    id = "id-1",
                    sourceRoutePatternId = routeSegment.sourceRoutePatternId,
                    sourceRouteId = routeSegment.sourceRouteId,
                    stopIds = listOf("davis", "porter"),
                    otherPatternsByStopId = mapOf(),
                    isAlerting = true
                ),
                AlertAwareRouteSegment(
                    id = "id-2",
                    sourceRoutePatternId = routeSegment.sourceRoutePatternId,
                    sourceRouteId = routeSegment.sourceRouteId,
                    stopIds = listOf("porter", "harvard", "central"),
                    otherPatternsByStopId = mapOf(),
                    isAlerting = false
                ),
            ),
            routeSegment.splitAlertingSegments(alertsForStop)
        )
    }

    @Test
    fun `splitAlertingSegments when alerting segment at the ends splits so alert in each segment`() {
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
                "alewife" to serviceAlert("alewife", routeSegment.sourceRouteId),
                "davis" to serviceAlert("davis", routeSegment.sourceRouteId),
                "central" to serviceAlert("alewife", routeSegment.sourceRouteId)
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
                    isAlerting = true
                ),
                AlertAwareRouteSegment(
                    id = "id-1",
                    sourceRoutePatternId = routeSegment.sourceRoutePatternId,
                    sourceRouteId = routeSegment.sourceRouteId,
                    stopIds = listOf("davis", "porter", "harvard", "central"),
                    otherPatternsByStopId = mapOf(),
                    isAlerting = false
                ),
                AlertAwareRouteSegment(
                    id = "id-2",
                    sourceRoutePatternId = routeSegment.sourceRoutePatternId,
                    sourceRouteId = routeSegment.sourceRouteId,
                    stopIds = listOf("central"),
                    otherPatternsByStopId = mapOf(),
                    isAlerting = true
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
                    isAlerting = true
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
                    isAlerting = false
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
