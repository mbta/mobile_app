package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertAssociatedStop
import com.mbta.tid.mbta_app.model.Line
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
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.GreenLineTestHelper
import com.mbta.tid.mbta_app.utils.isRoughlyEqualTo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.turf.misc.nearestPointTo
import org.maplibre.spatialk.turf.misc.slice

class RouteFeaturesBuilderTest {
    @Test
    fun `creates route source data`() = runBlocking {
        val routeSources =
            RouteFeaturesBuilder.generateRouteSources(
                routeData = MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
                globalData = MapTestDataHelper.global,
                alertsByStop = emptyMap(),
            )

        assertEquals(2, routeSources.size) // red, orange
        assertEquals(
            2,
            routeSources
                .first { it.routeId == MapTestDataHelper.routeRed.id }
                .features
                .features
                .size,
        )
        assertEquals(
            1,
            routeSources
                .first { it.routeId == MapTestDataHelper.routeOrange.id }
                .features
                .features
                .size,
        )

        assertEquals(
            LineString(Polyline.decode(MapTestDataHelper.shapeRedC2.polyline!!))
                .slice(
                    start = MapTestDataHelper.stopAlewife.position,
                    stop = MapTestDataHelper.stopDavis.position,
                ),
            routeSources
                .first { it.routeId == MapTestDataHelper.routeRed.id }
                .features
                .features
                .first()
                .geometry,
        )

        assertEquals(
            LineString(Polyline.decode(MapTestDataHelper.shapeOrangeC1.polyline!!))
                .slice(
                    start = MapTestDataHelper.stopAssembly.position,
                    stop = MapTestDataHelper.stopSullivan.position,
                ),
            routeSources
                .first { it.routeId == MapTestDataHelper.routeOrange.id }
                .features
                .features
                .first()
                .geometry,
        )
    }

    @Test
    fun `splits for alerts`() = runBlocking {
        val now = EasternTimeInstant.now()

        val objects = ObjectCollectionBuilder()

        val redAlert = objects.alert {
            id = "a1"
            effect = Alert.Effect.Shuttle
            informedEntity(
                listOf(Alert.InformedEntity.Activity.Board),
                route = MapTestDataHelper.routeRed.id.idText,
                routeType = RouteType.HEAVY_RAIL,
                stop = MapTestDataHelper.stopPorter.id,
            )
            informedEntity(
                listOf(Alert.InformedEntity.Activity.Board),
                route = MapTestDataHelper.routeRed.id.idText,
                routeType = RouteType.HEAVY_RAIL,
                stop = MapTestDataHelper.stopHarvard.id,
            )
        }
        val alertsByStop =
            mapOf(
                MapTestDataHelper.stopPorter.id to
                    AlertAssociatedStop(
                        stop = MapTestDataHelper.stopPorter,
                        relevantAlerts = listOf(redAlert),
                        stateByRoute = mapOf(MapStopRoute.RED to StopAlertState.Shuttle),
                        now = now,
                    ),
                MapTestDataHelper.stopHarvard.id to
                    AlertAssociatedStop(
                        stop = MapTestDataHelper.stopHarvard,
                        relevantAlerts = listOf(redAlert),
                        stateByRoute = mapOf(MapStopRoute.RED to StopAlertState.Shuttle),
                        now = now,
                    ),
            )

        val routeSources =
            RouteFeaturesBuilder.generateRouteSources(
                routeData = MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
                globalData = MapTestDataHelper.global,
                alertsByStop = alertsByStop,
            )

        val redSource = routeSources.first { it.routeId == MapTestDataHelper.routeRed.id }
        val redFeatures = redSource.features.features

        assertEquals(3, redFeatures.size)
        assertEquals(
            SegmentAlertState.Normal.name,
            redFeatures[0].properties[RouteFeaturesBuilder.propAlertStateKey],
        )
        assertEquals(
            LineString(Polyline.decode(MapTestDataHelper.shapeRedC1.polyline!!))
                .slice(
                    start = MapTestDataHelper.stopAlewife.position,
                    stop = MapTestDataHelper.stopDavis.position,
                ),
            redFeatures[0].geometry,
        )

        assertEquals(
            SegmentAlertState.Shuttle.name,
            redFeatures[1].properties[RouteFeaturesBuilder.propAlertStateKey],
        )
        assertEquals(
            LineString(Polyline.decode(MapTestDataHelper.shapeRedC1.polyline))
                .slice(
                    start = MapTestDataHelper.stopPorter.position,
                    stop = MapTestDataHelper.stopHarvard.position,
                ),
            redFeatures[1].geometry,
        )
        assertEquals(
            SegmentAlertState.Normal.name,
            redFeatures[2].properties[RouteFeaturesBuilder.propAlertStateKey],
        )
        assertEquals(
            LineString(Polyline.decode(MapTestDataHelper.shapeRedC1.polyline))
                .slice(
                    start = MapTestDataHelper.stopHarvard.position,
                    stop = MapTestDataHelper.stopCentral.position,
                ),
            redFeatures[2].geometry,
        )
    }

    @Test
    fun `uses full shape for loop segment`() = runBlocking {
        val loopRouteData =
            listOf(
                MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                    routeId = MapTestDataHelper.route67.id,
                    segmentedShapes =
                        listOf(
                            SegmentedRouteShape(
                                sourceRoutePatternId = MapTestDataHelper.pattern67.id,
                                sourceRouteId = MapTestDataHelper.pattern67.routeId,
                                directionId = MapTestDataHelper.pattern67.directionId,
                                routeSegments =
                                    listOf(
                                        RouteSegment(
                                            id = "loop-segment",
                                            sourceRoutePatternId = MapTestDataHelper.pattern67.id,
                                            sourceRouteId = MapTestDataHelper.pattern67.routeId,
                                            stopIds =
                                                listOf(
                                                    MapTestDataHelper.stopAlewife.id,
                                                    MapTestDataHelper.stopDavis.id,
                                                    MapTestDataHelper.stopAlewife.id,
                                                ),
                                            otherPatternsByStopId = emptyMap(),
                                        )
                                    ),
                                shape = MapTestDataHelper.shapeRedC1,
                            )
                        ),
                )
            )

        val routeSources =
            RouteFeaturesBuilder.generateRouteSources(
                routeData = loopRouteData,
                globalData = MapTestDataHelper.global,
                alertsByStop = emptyMap(),
            )

        val geometry = routeSources.single().features.features.single().geometry
        assertEquals(LineString(Polyline.decode(MapTestDataHelper.shapeRedC1.polyline!!)), geometry)
    }

    @Test
    fun `uses full shape for bus route`() = runBlocking {
        val busRouteData =
            listOf(
                MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                    routeId = MapTestDataHelper.route67.id,
                    segmentedShapes =
                        listOf(
                            SegmentedRouteShape(
                                sourceRoutePatternId = MapTestDataHelper.pattern67.id,
                                sourceRouteId = MapTestDataHelper.pattern67.routeId,
                                directionId = MapTestDataHelper.pattern67.directionId,
                                routeSegments =
                                    listOf(
                                        RouteSegment(
                                            id = "bus-segment",
                                            sourceRoutePatternId = MapTestDataHelper.pattern67.id,
                                            sourceRouteId = MapTestDataHelper.pattern67.routeId,
                                            stopIds =
                                                listOf(
                                                    MapTestDataHelper.stopAlewife.id,
                                                    MapTestDataHelper.stopDavis.id,
                                                ),
                                            otherPatternsByStopId = emptyMap(),
                                        )
                                    ),
                                shape = MapTestDataHelper.shapeRedC1,
                            )
                        ),
                )
            )

        val routeSources =
            RouteFeaturesBuilder.generateRouteSources(
                routeData = busRouteData,
                globalData = MapTestDataHelper.global,
                alertsByStop = emptyMap(),
            )

        val geometry = routeSources.single().features.features.single().geometry
        assertEquals(LineString(Polyline.decode(MapTestDataHelper.shapeRedC1.polyline!!)), geometry)
    }

    @Test
    fun `bus alert segments preserve leading and trailing shape tails`() = runBlocking {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()

        val busAlert = objects.alert {
            id = "bus-a1"
            effect = Alert.Effect.Shuttle
            informedEntity(
                listOf(Alert.InformedEntity.Activity.Board),
                route = MapTestDataHelper.route67.id.idText,
                routeType = RouteType.BUS,
                stop = MapTestDataHelper.stopPorter.id,
            )
            informedEntity(
                listOf(Alert.InformedEntity.Activity.Board),
                route = MapTestDataHelper.route67.id.idText,
                routeType = RouteType.BUS,
                stop = MapTestDataHelper.stopHarvard.id,
            )
        }

        val alertsByStop =
            mapOf(
                MapTestDataHelper.stopPorter.id to
                    AlertAssociatedStop(
                        stop = MapTestDataHelper.stopPorter,
                        relevantAlerts = listOf(busAlert),
                        stateByRoute = mapOf(MapStopRoute.BUS to StopAlertState.Shuttle),
                        now = now,
                    ),
                MapTestDataHelper.stopHarvard.id to
                    AlertAssociatedStop(
                        stop = MapTestDataHelper.stopHarvard,
                        relevantAlerts = listOf(busAlert),
                        stateByRoute = mapOf(MapStopRoute.BUS to StopAlertState.Shuttle),
                        now = now,
                    ),
            )

        val busRouteData =
            listOf(
                MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                    routeId = MapTestDataHelper.route67.id,
                    segmentedShapes =
                        listOf(
                            SegmentedRouteShape(
                                sourceRoutePatternId = MapTestDataHelper.pattern67.id,
                                sourceRouteId = MapTestDataHelper.pattern67.routeId,
                                directionId = MapTestDataHelper.pattern67.directionId,
                                routeSegments =
                                    listOf(
                                        RouteSegment(
                                            id = "bus-alert-segment",
                                            sourceRoutePatternId = MapTestDataHelper.pattern67.id,
                                            sourceRouteId = MapTestDataHelper.pattern67.routeId,
                                            stopIds =
                                                listOf(
                                                    MapTestDataHelper.stopAlewife.id,
                                                    MapTestDataHelper.stopDavis.id,
                                                    MapTestDataHelper.stopPorter.id,
                                                    MapTestDataHelper.stopHarvard.id,
                                                    MapTestDataHelper.stopCentral.id,
                                                ),
                                            otherPatternsByStopId = emptyMap(),
                                        )
                                    ),
                                shape = MapTestDataHelper.shapeRedC1,
                            )
                        ),
                )
            )

        val routeSources =
            RouteFeaturesBuilder.generateRouteSources(
                routeData = busRouteData,
                globalData = MapTestDataHelper.global,
                alertsByStop = alertsByStop,
            )

        val busFeatures = routeSources.single().features.features
        val fullCoordinates =
            LineString(Polyline.decode(MapTestDataHelper.shapeRedC1.polyline!!)).coordinates
        assertEquals(3, busFeatures.size)
        assertEquals(
            SegmentAlertState.Normal.name,
            busFeatures[0].properties[RouteFeaturesBuilder.propAlertStateKey],
        )
        assertEquals(
            SegmentAlertState.Shuttle.name,
            busFeatures[1].properties[RouteFeaturesBuilder.propAlertStateKey],
        )
        assertEquals(
            SegmentAlertState.Normal.name,
            busFeatures[2].properties[RouteFeaturesBuilder.propAlertStateKey],
        )

        val firstSegmentCoordinates = (busFeatures[0].geometry as LineString).coordinates
        val middleSegmentCoordinates = (busFeatures[1].geometry as LineString).coordinates
        val lastSegmentCoordinates = (busFeatures[2].geometry as LineString).coordinates

        val shape = LineString(Polyline.decode(MapTestDataHelper.shapeRedC1.polyline))
        val porterPositionOnLine =
            shape.nearestPointTo(MapTestDataHelper.stopPorter.position).geometry.coordinates
        val harvardPositionOnLine =
            shape.nearestPointTo(MapTestDataHelper.stopHarvard.position).geometry.coordinates

        assertEquals(firstSegmentCoordinates.first(), fullCoordinates.first())
        assertTrue(firstSegmentCoordinates.last().isRoughlyEqualTo(porterPositionOnLine))
        assertTrue(middleSegmentCoordinates.first().isRoughlyEqualTo(porterPositionOnLine))
        assertTrue(middleSegmentCoordinates.last().isRoughlyEqualTo(harvardPositionOnLine))
        assertTrue(lastSegmentCoordinates.first().isRoughlyEqualTo(harvardPositionOnLine))
        assertEquals(lastSegmentCoordinates.last(), fullCoordinates.last())
    }

    @Test
    fun `transforms shapes with stops`() {
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
                    ),
            )

        val transformedShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes> =
            RouteFeaturesBuilder.shapesWithStopsToMapFriendly(
                listOf(shapeWithStops),
                mapOf(
                    MapTestDataHelper.stopAlewife.id to MapTestDataHelper.stopAlewife,
                    MapTestDataHelper.stopDavis.id to MapTestDataHelper.stopDavis,
                    MapTestDataHelper.stopAlewifeChild.id to MapTestDataHelper.stopAlewifeChild,
                    MapTestDataHelper.stopDavisChild.id to MapTestDataHelper.stopDavisChild,
                ),
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
                                                    MapTestDataHelper.stopDavis.id,
                                                ),
                                            otherPatternsByStopId = emptyMap(),
                                        )
                                    ),
                                shape = shapeWithStops.shape,
                            )
                        ),
                )
            ),
        )
    }

    @Test
    fun testShapeFiltering() {
        val basicMapResponse =
            StopMapResponse(
                routeShapes = MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
                childStops = emptyMap(),
            )
        val filteredShapes =
            RouteFeaturesBuilder.filteredRouteShapesForStop(
                basicMapResponse,
                StopDetailsFilter(
                    MapTestDataHelper.routeRed.id,
                    MapTestDataHelper.patternRed10.directionId,
                ),
                null,
            )
        assertEquals(filteredShapes.count(), 1)

        val glFilteredShapes =
            RouteFeaturesBuilder.filteredRouteShapesForStop(
                GreenLineTestHelper.stopMapResponse,
                StopDetailsFilter(Line.Id("line-Green"), 0),
                null,
            )
        assertEquals(glFilteredShapes.count(), 3)
        assertEquals(glFilteredShapes.get(0).segmentedShapes.count(), 1)
    }
}
