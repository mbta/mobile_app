package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.model.MapStop
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.StopAlertState
import com.mbta.tid.mbta_app.utils.TestData
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class StopFeaturesBuilderTest {
    @Test
    fun `stop sources are created`() = runBlocking {
        val objects = TestData.clone()

        val stop1 = objects.getStop("place-aqucl")
        val stop2 = objects.getStop("place-armnl")
        val stop3 = objects.getStop("place-asmnl")
        val stop4 = objects.getStop("1432")
        val stop5 = objects.getStop("14320")
        val stop6 = objects.getStop("13")

        val collection =
            StopFeaturesBuilder.buildCollection(
                stops =
                    mapOf(
                        stop1.id to
                            MapStop(
                                stop = stop1,
                                routes = emptyMap(),
                                routeTypes = listOf(MapStopRoute.BLUE),
                                routeDirections = emptyMap(),
                                isTerminal = false,
                                alerts = null,
                            ),
                        stop2.id to
                            MapStop(
                                stop = stop2,
                                routes = emptyMap(),
                                routeTypes = listOf(MapStopRoute.GREEN),
                                routeDirections = emptyMap(),
                                isTerminal = false,
                                alerts = null,
                            ),
                        stop3.id to
                            MapStop(
                                stop = stop3,
                                routes = emptyMap(),
                                routeTypes =
                                    listOf(
                                        MapStopRoute.RED,
                                        MapStopRoute.MATTAPAN,
                                        MapStopRoute.BUS,
                                    ),
                                routeDirections = emptyMap(),
                                isTerminal = false,
                                alerts = null,
                            ),
                        stop4.id to
                            MapStop(
                                stop = stop4,
                                routes = emptyMap(),
                                routeTypes = listOf(MapStopRoute.BUS),
                                routeDirections = emptyMap(),
                                isTerminal = false,
                                alerts = null,
                            ),
                        stop5.id to
                            MapStop(
                                stop = stop5,
                                routes = emptyMap(),
                                routeTypes = listOf(MapStopRoute.BUS),
                                routeDirections = emptyMap(),
                                isTerminal = false,
                                alerts = null,
                            ),
                        stop6.id to
                            MapStop(
                                stop = stop6,
                                routes = emptyMap(),
                                routeTypes = listOf(MapStopRoute.BUS),
                                routeDirections = emptyMap(),
                                isTerminal = false,
                                alerts = null,
                            ),
                    ),
                routeSourceDetails = emptyList(),
            )

        assertEquals(5, collection.features.size)
        assertTrue(collection.features.any { it.geometry == Point(stop1.position) })
    }

    @Test
    fun `stops are snapped to routes`() = runBlocking {
        val stops =
            mapOf(
                MapTestDataHelper.stopAssembly.id to MapTestDataHelper.mapStopAssembly,
                MapTestDataHelper.stopSullivan.id to MapTestDataHelper.mapStopSullivan,
                MapTestDataHelper.stopAlewife.id to MapTestDataHelper.mapStopAlewife,
                MapTestDataHelper.stopDavis.id to MapTestDataHelper.mapStopDavis,
            )

        val routeLines =
            RouteFeaturesBuilder.generateRouteSources(
                routeData = MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
                stopsById = stops.mapValues { it.value.stop },
                alertsByStop = emptyMap(),
            )
        val collection =
            StopFeaturesBuilder.buildCollection(stops = stops, routeSourceDetails = routeLines)
        val snappedStopCoordinates =
            Position(latitude = 42.3961623851223, longitude = -71.14129664101432)

        assertEquals(4, collection.features.size)
        assertEquals(
            Point(snappedStopCoordinates),
            collection.features.find { it.id == MapTestDataHelper.stopAlewife.id }?.geometry,
        )
    }

    @Test
    fun `stops features have service status`() = runBlocking {
        val objects = MapTestDataHelper.objects

        val stops = objects.stops.filterKeys { it in setOf("70061", "place-alfcl", "place-astao") }

        val collection =
            StopFeaturesBuilder.buildCollection(
                stops =
                    mapOf(
                        "70061" to
                            MapStop(
                                stop = stops.getValue("70061"),
                                routes =
                                    mapOf(MapStopRoute.RED to listOf(MapTestDataHelper.routeRed)),
                                routeTypes = listOf(MapStopRoute.RED),
                                routeDirections =
                                    mapOf(MapTestDataHelper.routeRed.id to setOf(0, 1)),
                                isTerminal = true,
                                alerts = null,
                            ),
                        "place-alfcl" to
                            MapStop(
                                stop = stops.getValue("place-alfcl"),
                                routes =
                                    mapOf(MapStopRoute.RED to listOf(MapTestDataHelper.routeRed)),
                                routeTypes = listOf(MapStopRoute.RED),
                                routeDirections =
                                    mapOf(MapTestDataHelper.routeRed.id to setOf(0, 1)),
                                isTerminal = true,
                                alerts = mapOf(MapStopRoute.RED to StopAlertState.Shuttle),
                            ),
                        "place-astao" to
                            MapStop(
                                stop = stops.getValue("place-astao"),
                                routes =
                                    mapOf(
                                        MapStopRoute.ORANGE to listOf(MapTestDataHelper.routeOrange)
                                    ),
                                routeTypes = listOf(MapStopRoute.ORANGE),
                                routeDirections =
                                    mapOf(MapTestDataHelper.routeOrange.id to setOf(0, 1)),
                                isTerminal = false,
                                alerts = mapOf(MapStopRoute.ORANGE to StopAlertState.Suspension),
                            ),
                    ),
                routeSourceDetails = emptyList(),
            )

        assertEquals(2, collection.features.size)

        val alewifeFeature = collection.features.find { it.id == "place-alfcl" }
        assertNotNull(alewifeFeature)
        val alewifeServiceStatus =
            checkNotNull(alewifeFeature.properties[StopFeaturesBuilder.propServiceStatusKey])
        assertEquals(StopAlertState.Shuttle.name, alewifeServiceStatus[MapStopRoute.RED.name])

        val assemblyFeature = collection.features.find { it.id == "place-astao" }
        assertNotNull(assemblyFeature)
        val assemblyServiceStatus =
            checkNotNull(assemblyFeature.properties[StopFeaturesBuilder.propServiceStatusKey])
        assertEquals(
            StopAlertState.Suspension.name,
            assemblyServiceStatus[MapStopRoute.ORANGE.name],
        )
    }

    @Test
    fun `stop features have routes`() = runBlocking {
        val stops =
            mapOf(
                MapTestDataHelper.stopAssembly.id to MapTestDataHelper.mapStopAssembly,
                MapTestDataHelper.stopSullivan.id to MapTestDataHelper.mapStopSullivan,
                MapTestDataHelper.stopAlewife.id to MapTestDataHelper.mapStopAlewife,
                MapTestDataHelper.stopDavis.id to MapTestDataHelper.mapStopDavis,
            )

        val routeLines =
            RouteFeaturesBuilder.generateRouteSources(
                routeData = MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
                stopsById = stops.mapValues { it.value.stop },
                alertsByStop = emptyMap(),
            )
        val collection =
            StopFeaturesBuilder.buildCollection(stops = stops, routeSourceDetails = routeLines)

        assertEquals(4, collection.features.size)
        val assemblyFeature =
            collection.features.find { it.id == MapTestDataHelper.stopAssembly.id }

        val assemblyRoutes = assemblyFeature?.properties?.get(StopFeaturesBuilder.propMapRoutesKey)
        assertEquals(listOf(MapStopRoute.ORANGE.name), assemblyRoutes)

        val alewifeFeature = collection.features.find { it.id == MapTestDataHelper.stopAlewife.id }

        val alewifeRoutes = alewifeFeature?.properties?.get(StopFeaturesBuilder.propMapRoutesKey)
        assertEquals(listOf(MapStopRoute.RED.name, MapStopRoute.BUS.name), alewifeRoutes)
        val alewifeRouteIds = alewifeFeature?.properties?.get(StopFeaturesBuilder.propRouteIdsKey)
        assertEquals(
            mapOf(
                MapStopRoute.RED.name to listOf(MapTestDataHelper.routeRed.id.idText),
                MapStopRoute.BUS.name to listOf(MapTestDataHelper.route67.id.idText),
            ),
            alewifeRouteIds,
        )
    }

    @Test
    fun `stop features have names`() = runBlocking {
        val stops =
            mapOf(
                MapTestDataHelper.stopAssembly.id to MapTestDataHelper.mapStopAssembly,
                MapTestDataHelper.stopAlewife.id to MapTestDataHelper.mapStopAlewife,
            )

        val collection =
            StopFeaturesBuilder.buildCollection(stops = stops, routeSourceDetails = emptyList())

        assertEquals(2, collection.features.size)
        val assemblyFeature =
            collection.features.find { it.id == MapTestDataHelper.stopAssembly.id }

        val assemblyName = assemblyFeature?.properties?.get(StopFeaturesBuilder.propNameKey)
        assertEquals(MapTestDataHelper.stopAssembly.name, assemblyName)

        val alewifeFeature = collection.features.find { it.id == MapTestDataHelper.stopAlewife.id }

        val alewifeName = alewifeFeature?.properties?.get(StopFeaturesBuilder.propNameKey)
        assertEquals(MapTestDataHelper.stopAlewife.name, alewifeName)
    }

    @Test
    fun `stop features have terminals`() = runBlocking {
        val stops =
            mapOf(
                MapTestDataHelper.stopAlewife.id to MapTestDataHelper.mapStopAlewife,
                MapTestDataHelper.stopDavis.id to MapTestDataHelper.mapStopDavis,
            )

        val collection =
            StopFeaturesBuilder.buildCollection(stops = stops, routeSourceDetails = emptyList())

        assertEquals(2, collection.features.size)
        val alewifeFeature = collection.features.find { it.id == MapTestDataHelper.stopAlewife.id }

        val alewifeIsTerminal =
            alewifeFeature?.properties?.get(StopFeaturesBuilder.propIsTerminalKey)
        assertEquals(MapTestDataHelper.mapStopAlewife.isTerminal, alewifeIsTerminal)

        val davisFeature = collection.features.find { it.id == MapTestDataHelper.stopDavis.id }

        val davisIsTerminal = davisFeature?.properties?.get(StopFeaturesBuilder.propIsTerminalKey)
        assertEquals(MapTestDataHelper.mapStopDavis.isTerminal, davisIsTerminal)
    }
}
