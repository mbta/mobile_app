package com.mbta.tid.mbta_app.android.map

import com.mapbox.geojson.LineString
import com.mapbox.turf.TurfMisc.lineSlice
import com.mbta.tid.mbta_app.android.util.toPoint
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertAssociatedStop
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RouteSourceGeneratorTest {
    @Test
    fun testRouteSourcesAreCreated() {
        val routeSourceGenerator =
            RouteSourceGenerator(
                MapTestDataHelper.routeResponse,
                MapTestDataHelper.objects.stops,
                emptyMap()
            )
        assertEquals(2, routeSourceGenerator.routeSourceDetails.size)

        val redSource =
            routeSourceGenerator.routeSourceDetails.find {
                it.sourceId == RouteSourceGenerator.getRouteSourceId(MapTestDataHelper.routeRed.id)
            }
        assertNotNull(redSource)
        assertEquals(2, redSource!!.features.size)
        assertEquals(
            lineSlice(
                MapTestDataHelper.stopAlewife.position.toPoint(),
                MapTestDataHelper.stopDavis.position.toPoint(),
                LineString.fromPolyline(MapTestDataHelper.shapeRedC2.polyline!!, 5)
            ),
            redSource.features[0].geometry()
        )

        val orangeSource =
            routeSourceGenerator.routeSourceDetails.find {
                it.sourceId ==
                    RouteSourceGenerator.getRouteSourceId(MapTestDataHelper.routeOrange.id)
            }
        assertNotNull(orangeSource)
        assertEquals(1, orangeSource!!.features.size)
        assertEquals(
            lineSlice(
                MapTestDataHelper.stopAssembly.position.toPoint(),
                MapTestDataHelper.stopSullivan.position.toPoint(),
                LineString.fromPolyline(MapTestDataHelper.shapeOrangeC1.polyline!!, 5)
            ),
            orangeSource.features[0].geometry()
        )
    }

    fun testAlertingSourcesCreated() {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()

        val redAlert =
            objects.alert {
                id = "a1"
                effect = Alert.Effect.Shuttle
                activePeriod(now - 1.seconds, null)
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = MapTestDataHelper.routeRed.id,
                    routeType = RouteType.HEAVY_RAIL,
                    stop = MapTestDataHelper.stopPorter.id
                )
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = MapTestDataHelper.routeRed.id,
                    routeType = RouteType.HEAVY_RAIL,
                    stop = MapTestDataHelper.stopHarvard.id
                )
            }

        val alertsByStop =
            mapOf(
                MapTestDataHelper.stopPorter.id to
                    AlertAssociatedStop(
                        MapTestDataHelper.stopPorter,
                        listOf(redAlert),
                        listOf(MapTestDataHelper.patternRed30),
                        emptyMap(),
                        emptyMap()
                    ),
                MapTestDataHelper.stopHarvard.id to
                    AlertAssociatedStop(
                        MapTestDataHelper.stopHarvard,
                        listOf(redAlert),
                        listOf(MapTestDataHelper.patternRed30),
                        emptyMap(),
                        emptyMap()
                    )
            )

        val routeSourceGenerator =
            RouteSourceGenerator(
                MapTestDataHelper.routeResponse,
                MapTestDataHelper.objects.stops,
                alertsByStop
            )

        assertEquals(2, routeSourceGenerator.routeSourceDetails.size)

        val redSource = routeSourceGenerator.routeSourceDetails.first()
        assertNotNull(redSource)
        // Alewife - Davis (normal), Harvard - Porter (alerting), Porter - Central (normal)
        assertEquals(redSource.features.size, 3)

        assertEquals(
            false,
            redSource.features[0].getBooleanProperty(RouteSourceGenerator.propIsAlertingKey)
        )
        assertEquals(
            lineSlice(
                MapTestDataHelper.stopAlewife.position.toPoint(),
                MapTestDataHelper.stopDavis.position.toPoint(),
                LineString.fromPolyline(MapTestDataHelper.shapeRedC1.polyline!!, 5)
            ),
            redSource.features[0].geometry()
        )

        assertEquals(
            true,
            redSource.features[1].getBooleanProperty(RouteSourceGenerator.propIsAlertingKey)
        )
        assertEquals(
            lineSlice(
                MapTestDataHelper.stopPorter.position.toPoint(),
                MapTestDataHelper.stopHarvard.position.toPoint(),
                LineString.fromPolyline(MapTestDataHelper.shapeRedC1.polyline!!, 5)
            ),
            redSource.features[1].geometry()
        )

        assertEquals(
            false,
            redSource.features[2].getBooleanProperty(RouteSourceGenerator.propIsAlertingKey)
        )
        assertEquals(
            lineSlice(
                MapTestDataHelper.stopHarvard.position.toPoint(),
                MapTestDataHelper.stopCentral.position.toPoint(),
                LineString.fromPolyline(MapTestDataHelper.shapeRedC1.polyline!!, 5)
            ),
            redSource.features[2].geometry()
        )
    }
}
