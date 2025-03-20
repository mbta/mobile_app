package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertAssociatedStop
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteSegment
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SegmentAlertState
import com.mbta.tid.mbta_app.model.SegmentedRouteShape
import com.mbta.tid.mbta_app.model.StopAlertState
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.model.response.ShapeWithStops
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import com.mbta.tid.mbta_app.utils.GreenLineTestHelper
import io.github.dellisd.spatialk.geojson.LineString
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import io.github.dellisd.spatialk.turf.lineSlice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock

@OptIn(ExperimentalTurfApi::class)
class RouteFeaturesBuilderTest {
    @Test
    fun `creates route feature collection`() = runBlocking {
        val collection =
            RouteFeaturesBuilder.buildCollection(
                routeData = MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
                routesById = MapTestDataHelper.routesById,
                stopsById =
                    mapOf(
                        MapTestDataHelper.stopAlewife.id to MapTestDataHelper.stopAlewife,
                        MapTestDataHelper.stopDavis.id to MapTestDataHelper.stopDavis,
                        MapTestDataHelper.stopPorter.id to MapTestDataHelper.stopPorter,
                        MapTestDataHelper.stopHarvard.id to MapTestDataHelper.stopHarvard,
                        MapTestDataHelper.stopCentral.id to MapTestDataHelper.stopCentral,
                        MapTestDataHelper.stopAssembly.id to MapTestDataHelper.stopAssembly,
                        MapTestDataHelper.stopSullivan.id to MapTestDataHelper.stopSullivan
                    ),
                alertsByStop = emptyMap()
            )

        assertEquals(3, collection.features.size) // 2 red, 1 orange
        assertEquals(
            2,
            collection.features
                .filter {
                    it.properties[RouteFeaturesBuilder.propRouteId] == MapTestDataHelper.routeRed.id
                }
                .size
        )
        assertEquals(
            1,
            collection.features
                .filter {
                    it.properties[RouteFeaturesBuilder.propRouteId] ==
                        MapTestDataHelper.routeOrange.id
                }
                .size
        )

        assertEquals(
            lineSlice(
                start = MapTestDataHelper.stopAlewife.position,
                stop = MapTestDataHelper.stopDavis.position,
                line = LineString(Polyline.decode(MapTestDataHelper.shapeRedC2.polyline!!))
            ),
            collection.features
                .first {
                    it.properties[RouteFeaturesBuilder.propRouteId] == MapTestDataHelper.routeRed.id
                }
                .geometry
        )

        assertEquals(
            lineSlice(
                start = MapTestDataHelper.stopAssembly.position,
                stop = MapTestDataHelper.stopSullivan.position,
                line = LineString(Polyline.decode(MapTestDataHelper.shapeOrangeC1.polyline!!))
            ),
            collection.features
                .first {
                    it.properties[RouteFeaturesBuilder.propRouteId] ==
                        MapTestDataHelper.routeOrange.id
                }
                .geometry
        )
    }

    @Test
    fun `preserves route properties`() = runBlocking {
        val collection =
            RouteFeaturesBuilder.buildCollection(
                routeData = MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
                routesById = MapTestDataHelper.routesById,
                stopsById =
                    mapOf(
                        MapTestDataHelper.stopAlewife.id to MapTestDataHelper.stopAlewife,
                        MapTestDataHelper.stopDavis.id to MapTestDataHelper.stopDavis,
                        MapTestDataHelper.stopPorter.id to MapTestDataHelper.stopPorter,
                        MapTestDataHelper.stopHarvard.id to MapTestDataHelper.stopHarvard,
                        MapTestDataHelper.stopCentral.id to MapTestDataHelper.stopCentral,
                        MapTestDataHelper.stopAssembly.id to MapTestDataHelper.stopAssembly,
                        MapTestDataHelper.stopSullivan.id to MapTestDataHelper.stopSullivan
                    ),
                alertsByStop = emptyMap()
            )

        val firstRedFeature =
            collection.features
                .filter {
                    it.properties[RouteFeaturesBuilder.propRouteId] == MapTestDataHelper.routeRed.id
                }[0]

        assertEquals("#DA291C", firstRedFeature.properties[RouteFeaturesBuilder.propRouteColor])
        assertEquals("HEAVY_RAIL", firstRedFeature.properties[RouteFeaturesBuilder.propRouteType])
        assertEquals(-10010.0, firstRedFeature.properties[RouteFeaturesBuilder.propRouteSortKey])
    }

    @Test
    fun `splits for alerts`() = runBlocking {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()

        val redAlert =
            objects.alert {
                id = "a1"
                effect = Alert.Effect.Shuttle
                activePeriod(start = now - 1.seconds, end = null)
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
                        stop = MapTestDataHelper.stopPorter,
                        relevantAlerts = listOf(redAlert),
                        stateByRoute = mapOf(MapStopRoute.RED to StopAlertState.Shuttle)
                    ),
                MapTestDataHelper.stopHarvard.id to
                    AlertAssociatedStop(
                        stop = MapTestDataHelper.stopHarvard,
                        relevantAlerts = listOf(redAlert),
                        stateByRoute = mapOf(MapStopRoute.RED to StopAlertState.Shuttle)
                    ),
            )

        val collection =
            RouteFeaturesBuilder.buildCollection(
                routeData = MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
                routesById = MapTestDataHelper.routesById,
                stopsById =
                    mapOf(
                        MapTestDataHelper.stopAlewife.id to MapTestDataHelper.stopAlewife,
                        MapTestDataHelper.stopDavis.id to MapTestDataHelper.stopDavis,
                        MapTestDataHelper.stopPorter.id to MapTestDataHelper.stopPorter,
                        MapTestDataHelper.stopHarvard.id to MapTestDataHelper.stopHarvard,
                        MapTestDataHelper.stopCentral.id to MapTestDataHelper.stopCentral
                    ),
                alertsByStop = alertsByStop
            )

        val redFeatures =
            collection.features.filter {
                it.properties[RouteFeaturesBuilder.propRouteId] == MapTestDataHelper.routeRed.id
            }

        assertEquals(3, redFeatures.size)
        assertEquals(
            SegmentAlertState.Normal.name,
            redFeatures[0].properties[RouteFeaturesBuilder.propAlertStateKey]
        )
        assertEquals(
            lineSlice(
                start = MapTestDataHelper.stopAlewife.position,
                stop = MapTestDataHelper.stopDavis.position,
                line = LineString(Polyline.decode(MapTestDataHelper.shapeRedC1.polyline!!))
            ),
            redFeatures[0].geometry
        )

        assertEquals(
            SegmentAlertState.Shuttle.name,
            redFeatures[1].properties[RouteFeaturesBuilder.propAlertStateKey]
        )
        assertEquals(
            lineSlice(
                start = MapTestDataHelper.stopPorter.position,
                stop = MapTestDataHelper.stopHarvard.position,
                line = LineString(Polyline.decode(MapTestDataHelper.shapeRedC1.polyline!!))
            ),
            redFeatures[1].geometry,
        )
        assertEquals(
            SegmentAlertState.Normal.name,
            redFeatures[2].properties[RouteFeaturesBuilder.propAlertStateKey]
        )
        assertEquals(
            lineSlice(
                start = MapTestDataHelper.stopHarvard.position,
                stop = MapTestDataHelper.stopCentral.position,
                line = LineString(Polyline.decode(MapTestDataHelper.shapeRedC1.polyline!!))
            ),
            redFeatures[2].geometry
        )
    }

    @Test
    fun `transforms shapes with stops`() {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()

        val redAlert =
            objects.alert {
                id = "a1"
                effect = Alert.Effect.Shuttle
                activePeriod(start = now - 1.seconds, end = null)
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = MapTestDataHelper.routeRed.id,
                    routeType = RouteType.HEAVY_RAIL,
                    stop = MapTestDataHelper.stopAlewife.id
                )
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.Board),
                    route = MapTestDataHelper.routeRed.id,
                    routeType = RouteType.HEAVY_RAIL,
                    stop = MapTestDataHelper.stopDavis.id
                )
            }
        val alertsByStop =
            listOf(
                MapTestDataHelper.stopAlewife.id to
                    AlertAssociatedStop(
                        stop = MapTestDataHelper.stopAlewife,
                        relevantAlerts = listOf(redAlert),
                        stateByRoute = mapOf(MapStopRoute.RED to StopAlertState.Shuttle)
                    ),
                MapTestDataHelper.stopDavis.id to
                    AlertAssociatedStop(
                        stop = MapTestDataHelper.stopDavis,
                        relevantAlerts = listOf(redAlert),
                        stateByRoute = mapOf(MapStopRoute.RED to StopAlertState.Shuttle)
                    ),
            )

        val shapeWithStops =
            ShapeWithStops(
                directionId = MapTestDataHelper.patternRed10.directionId,
                routeId = MapTestDataHelper.routeRed.id,
                routePatternId = MapTestDataHelper.patternRed10.id,
                shape = MapTestDataHelper.shapeRedC1,
                stopIds =
                    listOf(
                        MapTestDataHelper.stopAlewifeChild.id,
                        MapTestDataHelper.stopDavisChild.id,
                    )
            )

        val transformedShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes> =
            RouteFeaturesBuilder.shapesWithStopsToMapFriendly(
                listOf(shapeWithStops),
                mapOf(
                    MapTestDataHelper.stopAlewife.id to MapTestDataHelper.stopAlewife,
                    MapTestDataHelper.stopDavis.id to MapTestDataHelper.stopDavis,
                    MapTestDataHelper.stopAlewifeChild.id to MapTestDataHelper.stopAlewifeChild,
                    MapTestDataHelper.stopDavisChild.id to MapTestDataHelper.stopDavisChild
                )
            )

        assertEquals(
            transformedShapes,
            listOf(
                MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                    routeId = shapeWithStops.routeId,
                    segmentedShapes =
                        listOf(
                            SegmentedRouteShape(
                                sourceRoutePatternId = shapeWithStops.routePatternId,
                                sourceRouteId = shapeWithStops.routeId,
                                directionId = shapeWithStops.directionId,
                                routeSegments =
                                    listOf(
                                        RouteSegment(
                                            id = shapeWithStops.shape!!.id,
                                            sourceRoutePatternId = shapeWithStops.routePatternId,
                                            sourceRouteId = shapeWithStops.routeId,
                                            stopIds =
                                                listOf(
                                                    MapTestDataHelper.stopAlewife.id,
                                                    MapTestDataHelper.stopDavis.id
                                                ),
                                            otherPatternsByStopId = emptyMap()
                                        ),
                                    ),
                                shape = shapeWithStops.shape!!
                            ),
                        )
                )
            )
        )
    }

    @Test
    fun testShapeFiltering() {
        val basicMapResponse =
            StopMapResponse(
                routeShapes = MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
                childStops = emptyMap()
            )
        val filteredShapes =
            RouteFeaturesBuilder.filteredRouteShapesForStop(
                basicMapResponse,
                StopDetailsFilter(
                    MapTestDataHelper.routeRed.id,
                    MapTestDataHelper.patternRed10.directionId
                ),
                null
            )
        assertEquals(filteredShapes.count(), 1)

        val glFilteredShapes =
            RouteFeaturesBuilder.filteredRouteShapesForStop(
                GreenLineTestHelper.stopMapResponse,
                StopDetailsFilter("line-Green", 0),
                null
            )
        assertEquals(glFilteredShapes.count(), 3)
        assertEquals(glFilteredShapes.get(0).segmentedShapes.count(), 1)
    }
}
