package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class AlertAssociatedStopTest {
    @Test
    fun `shows alert icon at boundary`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { id = "Red" }
        val routePattern = objects.routePattern(route)
        var platform1: Stop? = null
        var platform2: Stop? = null
        val stop =
            objects.stop {
                platform1 = childStop()
                platform2 = childStop()
            }
        val alert =
            objects.alert {
                activePeriod(EasternTimeInstant(Instant.DISTANT_PAST), null)
                effect = Alert.Effect.Shuttle
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board, Alert.InformedEntity.Activity.Ride),
                    route = route.id,
                    routeType = route.type,
                    stop = platform1!!.id,
                )
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board, Alert.InformedEntity.Activity.Ride),
                    route = route.id,
                    routeType = route.type,
                    stop = stop.id,
                )
            }

        val result =
            AlertAssociatedStop(
                stop,
                mapOf(platform1!!.id to setOf(alert), stop.id to setOf(alert)),
                setOf(),
                GlobalResponse(
                    objects,
                    mapOf(
                        platform1!!.id to listOf(routePattern.id),
                        platform2!!.id to listOf(routePattern.id),
                    ),
                ),
            )
        assertEquals(mapOf(MapStopRoute.RED to StopAlertState.Shuttle), result.stateByRoute)
    }

    @Test
    fun `handles full-route shuttle`() {
        val objects = ObjectCollectionBuilder()
        lateinit var child1: Stop
        lateinit var child2: Stop
        val stop =
            objects.stop {
                child1 = childStop()
                child2 = childStop()
            }
        val route =
            objects.route {
                id = "Mattapan"
                type = RouteType.LIGHT_RAIL
            }
        objects.routePattern(route) {
            directionId = 0
            representativeTrip { stopIds = listOf(child1.id) }
        }
        objects.routePattern(route) {
            directionId = 1
            representativeTrip { stopIds = listOf(child2.id) }
        }

        val alert =
            objects.alert {
                effect = Alert.Effect.Shuttle
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride,
                    ),
                    route = route.id,
                    routeType = route.type,
                )
            }

        val result = AlertAssociatedStop(stop, emptyMap(), setOf(alert), GlobalResponse(objects))

        assertEquals(mapOf(MapStopRoute.MATTAPAN to StopAlertState.Shuttle), result.stateByRoute)
        assertEquals(listOf(alert), result.childAlerts[child1.id]!!.serviceAlerts)
        assertEquals(listOf(alert), result.childAlerts[child2.id]!!.serviceAlerts)
    }
}
