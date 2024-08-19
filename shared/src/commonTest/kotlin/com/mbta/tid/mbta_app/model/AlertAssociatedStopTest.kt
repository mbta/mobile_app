package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Instant

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
                activePeriod(Instant.DISTANT_PAST, null)
                effect = Alert.Effect.Shuttle
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board, Alert.InformedEntity.Activity.Ride),
                    route = route.id,
                    routeType = route.type,
                    stop = platform1!!.id
                )
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board, Alert.InformedEntity.Activity.Ride),
                    route = route.id,
                    routeType = route.type,
                    stop = stop.id
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
                        platform2!!.id to listOf(routePattern.id)
                    )
                )
            )
        assertEquals(mapOf(MapStopRoute.RED to StopAlertState.Shuttle), result.stateByRoute)
    }
}
