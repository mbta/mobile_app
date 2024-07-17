package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.map.MapTestDataHelper.routesById
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.MapStop
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.StopAlertState
import io.github.dellisd.spatialk.geojson.Feature
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement

class StopFeaturesBuilderTest {
    @Test
    fun `stop sources are created`() {
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

        val collection =
            StopFeaturesBuilder.generateStopSource(
                stopData = StopSourceData(selectedStopId = null),
                stops =
                    mapOf(
                        stop1.id to
                            MapStop(
                                stop = stop1,
                                routes = emptyMap(),
                                routeTypes = listOf(MapStopRoute.BLUE),
                                isTerminal = false,
                                alerts = null
                            ),
                        stop2.id to
                            MapStop(
                                stop = stop2,
                                routes = emptyMap(),
                                routeTypes = listOf(MapStopRoute.GREEN),
                                isTerminal = false,
                                alerts = null
                            ),
                        stop3.id to
                            MapStop(
                                stop = stop3,
                                routes = emptyMap(),
                                routeTypes =
                                    listOf(
                                        MapStopRoute.RED,
                                        MapStopRoute.MATTAPAN,
                                        MapStopRoute.BUS
                                    ),
                                isTerminal = false,
                                alerts = null
                            ),
                        stop4.id to
                            MapStop(
                                stop = stop4,
                                routes = emptyMap(),
                                routeTypes = listOf(MapStopRoute.BUS),
                                isTerminal = false,
                                alerts = null
                            ),
                        stop5.id to
                            MapStop(
                                stop = stop5,
                                routes = emptyMap(),
                                routeTypes = listOf(MapStopRoute.BUS),
                                isTerminal = false,
                                alerts = null
                            ),
                        stop6.id to
                            MapStop(
                                stop = stop6,
                                routes = emptyMap(),
                                routeTypes = listOf(MapStopRoute.BUS),
                                isTerminal = false,
                                alerts = null
                            ),
                    ),
                linesToSnap = emptyList()
            )

        assertEquals(5, collection.features.size)
        assertTrue(collection.features.any { it.geometry == Point(stop1.position) })
    }

    @Test
    fun `stops are snapped to routes`() {
        val stops =
            mapOf(
                MapTestDataHelper.stopAssembly.id to MapTestDataHelper.mapStopAssembly,
                MapTestDataHelper.stopSullivan.id to MapTestDataHelper.mapStopSullivan,
                MapTestDataHelper.stopAlewife.id to MapTestDataHelper.mapStopAlewife,
                MapTestDataHelper.stopDavis.id to MapTestDataHelper.mapStopDavis,
            )

        val routeLines =
            RouteFeaturesBuilder.generateRouteLines(
                routeData = MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
                routesById = routesById,
                stopsById = stops.mapValues { it.value.stop },
                alertsByStop = emptyMap()
            )
        val collection =
            StopFeaturesBuilder.generateStopSource(
                stopData = StopSourceData(),
                stops = stops,
                linesToSnap = routeLines
            )
        val snappedStopCoordinates =
            Position(latitude = 42.3961623851223, longitude = -71.14129664101432)

        assertEquals(4, collection.features.size)
        assertEquals(
            Point(snappedStopCoordinates),
            collection.features.find { it.id == MapTestDataHelper.stopAlewife.id }?.geometry
        )
    }

    @Test
    fun `selected stop has prop set`() {
        val objects = ObjectCollectionBuilder()
        val selectedStop =
            objects.stop {
                id = "place-alfcl"
                name = "Alewife"
                latitude = 42.39583
                longitude = -71.141287
                locationType = LocationType.STATION
                childStopIds = listOf("70061")
            }

        val otherStop =
            objects.stop {
                id = "place-davis"
                name = "Davis"
                locationType = LocationType.STATION
                childStopIds = emptyList()
            }

        val collection =
            StopFeaturesBuilder.generateStopSource(
                stopData = StopSourceData(selectedStopId = selectedStop.id),
                stops =
                    mapOf(selectedStop.id to selectedStop, otherStop.id to otherStop).mapValues {
                        (_, stop) ->
                        MapStop(
                            stop = stop,
                            routes = mapOf(MapStopRoute.RED to listOf(MapTestDataHelper.routeRed)),
                            routeTypes = listOf(MapStopRoute.RED),
                            isTerminal = false,
                            alerts = null
                        )
                    },
                linesToSnap = emptyList()
            )

        assertEquals(2, collection.features.size)

        val selectedFeature = collection.features.find { it.id == selectedStop.id }
        val otherFeature = collection.features.find { it.id == otherStop.id }
        assertNotNull(selectedFeature)
        assertEquals(
            true,
            selectedFeature.getBooleanProperty(StopFeaturesBuilder.propIsSelectedKey)
        )

        assertEquals(false, otherFeature?.getBooleanProperty(StopFeaturesBuilder.propIsSelectedKey))
    }

    @Test
    fun `filtered stop ids`() {
        val collection =
            StopFeaturesBuilder.generateStopSource(
                stopData =
                    StopSourceData(
                        filteredStopIds = listOf(MapTestDataHelper.stopAlewife.id),
                        selectedStopId = null
                    ),
                stops =
                    mapOf(
                            MapTestDataHelper.stopAlewife.id to MapTestDataHelper.stopAlewife,
                            MapTestDataHelper.stopDavis.id to MapTestDataHelper.stopDavis
                        )
                        .mapValues { (_, stop) ->
                            MapStop(
                                stop = stop,
                                routes =
                                    mapOf(MapStopRoute.RED to listOf(MapTestDataHelper.routeRed)),
                                routeTypes = listOf(MapStopRoute.RED),
                                isTerminal = false,
                                alerts = null
                            )
                        },
                linesToSnap = emptyList()
            )

        assertEquals(1, collection.features.size)

        assertNotNull(collection.features.find { it.id == MapTestDataHelper.stopAlewife.id })
    }

    @Test
    fun `stops features have service status`() {
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

        val collection =
            StopFeaturesBuilder.generateStopSource(
                stopData = StopSourceData(),
                stops =
                    mapOf(
                        "70061" to
                            MapStop(
                                stop = stops.getValue("70061"),
                                routes =
                                    mapOf(MapStopRoute.RED to listOf(MapTestDataHelper.routeRed)),
                                routeTypes = listOf(MapStopRoute.RED),
                                isTerminal = true,
                                alerts = null
                            ),
                        "place-alfcl" to
                            MapStop(
                                stop = stops.getValue("place-alfcl"),
                                routes =
                                    mapOf(MapStopRoute.RED to listOf(MapTestDataHelper.routeRed)),
                                routeTypes = listOf(MapStopRoute.RED),
                                isTerminal = true,
                                alerts = mapOf(MapStopRoute.RED to StopAlertState.Shuttle)
                            ),
                        "place-astao" to
                            MapStop(
                                stop = stops.getValue("place-astao"),
                                routes =
                                    mapOf(
                                        MapStopRoute.ORANGE to listOf(MapTestDataHelper.routeOrange)
                                    ),
                                routeTypes = listOf(MapStopRoute.ORANGE),
                                isTerminal = false,
                                alerts = mapOf(MapStopRoute.ORANGE to StopAlertState.Suspension)
                            ),
                    ),
                linesToSnap = emptyList()
            )

        assertEquals(2, collection.features.size)

        val alewifeFeature = collection.features.find { it.id == "place-alfcl" }
        assertNotNull(alewifeFeature)
        val alewifeServiceStatus: Map<MapStopRoute, StopAlertState> =
            alewifeFeature.getDecodedJsonProperty(StopFeaturesBuilder.propServiceStatusKey)
        assertEquals(StopAlertState.Shuttle, alewifeServiceStatus[MapStopRoute.RED])

        val assemblyFeature = collection.features.find { it.id == "place-astao" }
        assertNotNull(assemblyFeature)
        val assemblyServiceStatus: Map<MapStopRoute, StopAlertState> =
            assemblyFeature.getDecodedJsonProperty(StopFeaturesBuilder.propServiceStatusKey)
        assertEquals(StopAlertState.Suspension, assemblyServiceStatus[MapStopRoute.ORANGE])
    }

    @Test
    fun `stop features have routes`() {
        val stops =
            mapOf(
                MapTestDataHelper.stopAssembly.id to MapTestDataHelper.mapStopAssembly,
                MapTestDataHelper.stopSullivan.id to MapTestDataHelper.mapStopSullivan,
                MapTestDataHelper.stopAlewife.id to MapTestDataHelper.mapStopAlewife,
                MapTestDataHelper.stopDavis.id to MapTestDataHelper.mapStopDavis,
            )

        val routeLines =
            RouteFeaturesBuilder.generateRouteLines(
                routeData = MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
                routesById = routesById,
                stopsById = stops.mapValues { it.value.stop },
                alertsByStop = emptyMap()
            )
        val collection =
            StopFeaturesBuilder.generateStopSource(
                stopData = StopSourceData(),
                stops = stops,
                linesToSnap = routeLines
            )

        assertEquals(4, collection.features.size)
        val assemblyFeature =
            collection.features.find { it.id == MapTestDataHelper.stopAssembly.id }

        val assemblyRoutes: List<MapStopRoute>? =
            assemblyFeature?.getDecodedJsonProperty(StopFeaturesBuilder.propMapRoutesKey)
        assertEquals(listOf(MapStopRoute.ORANGE), assemblyRoutes)

        val alewifeFeature = collection.features.find { it.id == MapTestDataHelper.stopAlewife.id }

        val alewifeRoutes: List<MapStopRoute>? =
            alewifeFeature?.getDecodedJsonProperty(StopFeaturesBuilder.propMapRoutesKey)
        assertEquals(listOf(MapStopRoute.RED, MapStopRoute.BUS), alewifeRoutes)
        val alewifeRouteIds: Map<MapStopRoute, List<String>>? =
            alewifeFeature?.getDecodedJsonProperty(StopFeaturesBuilder.propRouteIdsKey)
        assertEquals(
            mapOf(
                MapStopRoute.RED to listOf(MapTestDataHelper.routeRed.id),
                MapStopRoute.BUS to listOf(MapTestDataHelper.route67.id)
            ),
            alewifeRouteIds
        )
    }

    @Test
    fun `stop features have names`() {
        val stops =
            mapOf(
                MapTestDataHelper.stopAssembly.id to MapTestDataHelper.mapStopAssembly,
                MapTestDataHelper.stopAlewife.id to MapTestDataHelper.mapStopAlewife
            )

        val collection =
            StopFeaturesBuilder.generateStopSource(
                stopData = StopSourceData(),
                stops = stops,
                linesToSnap = emptyList()
            )

        assertEquals(2, collection.features.size)
        val assemblyFeature =
            collection.features.find { it.id == MapTestDataHelper.stopAssembly.id }

        val assemblyName = assemblyFeature?.getStringProperty(StopFeaturesBuilder.propNameKey)
        assertEquals(MapTestDataHelper.stopAssembly.name, assemblyName)

        val alewifeFeature = collection.features.find { it.id == MapTestDataHelper.stopAlewife.id }

        val alewifeName = alewifeFeature?.getStringProperty(StopFeaturesBuilder.propNameKey)
        assertEquals(MapTestDataHelper.stopAlewife.name, alewifeName)
    }

    @Test
    fun `stop features have terminals`() {
        val stops =
            mapOf(
                MapTestDataHelper.stopAlewife.id to MapTestDataHelper.mapStopAlewife,
                MapTestDataHelper.stopDavis.id to MapTestDataHelper.mapStopDavis
            )

        val collection =
            StopFeaturesBuilder.generateStopSource(
                stopData = StopSourceData(),
                stops = stops,
                linesToSnap = emptyList()
            )

        assertEquals(2, collection.features.size)
        val alewifeFeature = collection.features.find { it.id == MapTestDataHelper.stopAlewife.id }

        val alewifeIsTerminal =
            alewifeFeature?.getBooleanProperty(StopFeaturesBuilder.propIsTerminalKey)
        assertEquals(MapTestDataHelper.mapStopAlewife.isTerminal, alewifeIsTerminal)

        val davisFeature = collection.features.find { it.id == MapTestDataHelper.stopDavis.id }

        val davisIsTerminal =
            davisFeature?.getBooleanProperty(StopFeaturesBuilder.propIsTerminalKey)
        assertEquals(MapTestDataHelper.mapStopDavis.isTerminal, davisIsTerminal)
    }

    private inline fun <reified T> Feature.getDecodedJsonProperty(key: String) =
        json.decodeFromJsonElement<T>(getJsonProperty(key) ?: JsonNull)
}
