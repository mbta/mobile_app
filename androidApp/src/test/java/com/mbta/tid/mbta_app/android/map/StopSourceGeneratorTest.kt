package com.mbta.tid.mbta_app.android.map

import com.mapbox.geojson.Point
import com.mbta.tid.mbta_app.android.util.toPoint
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertAssociatedStop
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopAlertState
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StopSourceGeneratorTest {
    @Test
    fun testStopSourcesAreCreated() {
        val objects = ObjectCollectionBuilder()

        val stop1 =
            objects.stop {
                id = "place-aqucl"
                name = "Aquarium"
                latitude = 42.359784
                longitude = -71.051652
                locationType = LocationType.STATION
            }
        val stop2 =
            objects.stop {
                id = "place-armnl"
                name = "Arlington"
                latitude = 42.351902
                longitude = -71.070893
                locationType = LocationType.STATION
            }
        val stop3 =
            objects.stop {
                id = "place-asmnl"
                name = "Ashmont"
                latitude = 42.28452
                longitude = -71.063777
                locationType = LocationType.STATION
            }
        val stop4 =
            objects.stop {
                id = "1432"
                name = "Arsenal St @ Irving St"
                latitude = 42.364737
                longitude = -71.178564
                locationType = LocationType.STOP
            }
        val stop5 =
            objects.stop {
                id = "14320"
                name = "Adams St @ Whitwell St"
                latitude = 42.253069
                longitude = -71.017292
                locationType = LocationType.STOP
            }
        val stop6 =
            objects.stop {
                id = "13"
                name = "Andrew"
                latitude = 42.329962
                longitude = -71.057625
                locationType = LocationType.STOP
                parentStationId = "place-andrw"
            }

        val stopSourceGenerator =
            StopSourceGenerator(
                mapOf(
                    stop1.id to stop1,
                    stop2.id to stop2,
                    stop3.id to stop3,
                    stop4.id to stop4,
                    stop5.id to stop5,
                    stop6.id to stop6
                ),
                null,
                null
            )
        val sources = stopSourceGenerator.stopSources
        assertEquals(2, sources.size)

        val sourceIds = sources.map { it.sourceId }
        assertTrue(sourceIds.contains(StopSourceGenerator.getStopSourceId(LocationType.STATION)))
        assertTrue(sourceIds.contains(StopSourceGenerator.getStopSourceId(LocationType.STOP)))

        val stationSource =
            sources.first {
                it.sourceId == StopSourceGenerator.getStopSourceId(LocationType.STATION)
            }
        assertNotNull(stationSource)
        assertEquals(3, stationSource.features.size)
        assertTrue(stationSource.features.any { it.geometry() == stop1.position.toPoint() })

        val stopSource =
            sources.first { it.sourceId == StopSourceGenerator.getStopSourceId(LocationType.STOP) }
        assertNotNull(stopSource)
        assertEquals(2, stopSource.features.size)
        assertEquals(
            StopAlertState.Normal.name,
            stopSource.features[0].getStringProperty(StopSourceGenerator.propServiceStatusKey)
        )
        assertTrue(stopSource.features.any { it.geometry() == stop4.position.toPoint() })
    }

    @Test
    fun testStopsAreSnappedToRoutes() {
        val stops =
            MapTestDataHelper.objects.stops.filterKeys {
                it in
                    listOf(
                        MapTestDataHelper.stopAssembly.id,
                        MapTestDataHelper.stopSullivan.id,
                        MapTestDataHelper.stopAlewife.id,
                        MapTestDataHelper.stopDavis.id
                    )
            }

        val routeSourceGenerator =
            RouteSourceGenerator(MapTestDataHelper.routeResponse, stops, emptyMap())
        val stopSourceGenerator =
            StopSourceGenerator(stops, routeSourceGenerator.routeSourceDetails, null)
        val sources = stopSourceGenerator.stopSources
        val snappedStopCoordinates = Point.fromLngLat(-71.14129664101432, 42.3961623851223)

        val stationSource =
            sources.find {
                it.sourceId == StopSourceGenerator.getStopSourceId(LocationType.STATION)
            }
        assertNotNull(stationSource)
        assertEquals(4, stationSource!!.features.size)
        assertEquals(
            snappedStopCoordinates,
            stationSource.features.find { it.id() == MapTestDataHelper.stopAlewife.id }!!.geometry()
        )
    }

    fun testStopsFeaturesHaveServiceStatus() {
        val objects = MapTestDataHelper.objects

        val stops =
            mapOf(
                "70061" to
                    objects.stop {
                        id = "70061"
                        name = "Alewife"
                        latitude = 42.396158
                        longitude = -71.139971
                        locationType = LocationType.STOP
                        parentStationId = "place-alfcl"
                    },
                "place-alfcl" to
                    objects.stop {
                        id = "place-alfcl"
                        name = "Alewife"
                        latitude = 42.39583
                        longitude = -71.141287
                        locationType = LocationType.STATION
                        childStopIds = listOf("70061")
                    },
                "place-astao" to
                    objects.stop {
                        id = "place-astao"
                        name = "Assembly"
                        latitude = 42.392811
                        longitude = -71.077257
                        locationType = LocationType.STATION
                    },
            )

        val now = Clock.System.now()

        val redAlert =
            objects.alert {
                id = "a1"
                effect = Alert.Effect.Shuttle
                activePeriod(now - 1.seconds, null)
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = "Red",
                    routeType = RouteType.HEAVY_RAIL,
                    stop = "70061"
                )
            }
        val orangeAlert =
            objects.alert {
                id = "a2"
                effect = Alert.Effect.StationClosure
                activePeriod(now - 1.seconds, null)
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = "Orange",
                    routeType = RouteType.HEAVY_RAIL,
                    stop = "place-astao"
                )
            }

        val alertsByStop =
            mapOf(
                "place-alfcl" to
                    AlertAssociatedStop(
                        stops["place-alfcl"]!!,
                        listOf(),
                        listOf(MapTestDataHelper.patternRed30),
                        mapOf("70061" to stops["70061"]!!),
                        mapOf(
                            "70061" to
                                AlertAssociatedStop(
                                    stops["70061"]!!,
                                    listOf(redAlert),
                                    listOf(MapTestDataHelper.patternRed10),
                                    emptyMap(),
                                    emptyMap()
                                )
                        )
                    ),
                "place-astao" to
                    AlertAssociatedStop(
                        stops["place-astao"]!!,
                        listOf(orangeAlert),
                        listOf(MapTestDataHelper.patternOrange30),
                        emptyMap(),
                        emptyMap()
                    ),
            )
        val stopSourceGenerator = StopSourceGenerator(stops, null, alertsByStop)
        val sources = stopSourceGenerator.stopSources

        val stationSource =
            sources.first {
                it.sourceId == StopSourceGenerator.getStopSourceId(LocationType.STATION)
            }
        assertNotNull(stationSource)
        assertEquals(2, stationSource.features.size)

        val alewifeFeature =
            stationSource.features.first { feat ->
                feat.getStringProperty(StopSourceGenerator.propIdKey) == "place-alfcl"
            }
        assertNotNull(alewifeFeature)
        assertEquals(
            StopAlertState.PARTIAL_SERVICE.name,
            alewifeFeature.getStringProperty(StopSourceGenerator.propServiceStatusKey)
        )

        val assemblyFeature =
            stationSource.features.first { feat ->
                feat.getStringProperty(StopSourceGenerator.propIdKey) == "place-astao"
            }
        assertNotNull(assemblyFeature)
        assertEquals(
            StopAlertState.NO_SERVICE.name,
            assemblyFeature.getStringProperty(StopSourceGenerator.propServiceStatusKey)
        )
    }
}
