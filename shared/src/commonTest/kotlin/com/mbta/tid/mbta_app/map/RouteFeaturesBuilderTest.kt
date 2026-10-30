package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertAssociatedStop
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.GlobalMapData
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RoutePatternKey
import com.mbta.tid.mbta_app.model.RouteSegment
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SegmentAlertState
import com.mbta.tid.mbta_app.model.SegmentedRouteShape
import com.mbta.tid.mbta_app.model.Shape
import com.mbta.tid.mbta_app.model.StopAlertState
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.model.response.ShapeWithStops
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.GreenLineTestHelper
import com.mbta.tid.mbta_app.utils.TestData
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
    fun `commuter rail keeps shared stops normal only when another visible route serves them`() =
        runBlocking {
            val now = EasternTimeInstant.now()

            fun alertStatesFor(routeType: RouteType, otherRouteVisible: Boolean): List<String?> =
                runBlocking {
                    val objects = MapTestDataHelper.objects.clone()
                    val route = objects.route { type = routeType }
                    val otherRoute = objects.route { type = routeType }
                    val stops =
                        listOf(
                            MapTestDataHelper.stopAlewife,
                            MapTestDataHelper.stopDavis,
                            MapTestDataHelper.stopPorter,
                            MapTestDataHelper.stopHarvard,
                            MapTestDataHelper.stopCentral,
                        )
                    val alert = objects.alert {
                        effect = Alert.Effect.Suspension
                        stops.take(4).forEach {
                            informedEntity(
                                listOf(Alert.InformedEntity.Activity.Board),
                                route = route.id.idText,
                                routeType = routeType,
                                stop = it.id,
                            )
                        }
                    }
                    val alertsByStop =
                        stops.take(4).associate {
                            it.id to
                                AlertAssociatedStop(
                                    stop = it,
                                    relevantAlerts = listOf(alert),
                                    stateByRoute = emptyMap(),
                                    now = now,
                                )
                        }

                    fun routeWithShape(
                        routeId: Route.Id,
                        stopIds: List<String>,
                        otherPatternsByStopId: Map<String, List<RoutePatternKey>>,
                    ) =
                        MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                            routeId = routeId,
                            segmentedShapes =
                                listOf(
                                    SegmentedRouteShape(
                                        sourceRoutePatternId = "$routeId-pattern",
                                        sourceRouteId = routeId,
                                        directionId = 0,
                                        routeSegments =
                                            listOf(
                                                RouteSegment(
                                                    id = "$routeId-segment",
                                                    sourceRoutePatternId = "$routeId-pattern",
                                                    sourceRouteId = routeId,
                                                    stopIds = stopIds,
                                                    otherPatternsByStopId = otherPatternsByStopId,
                                                )
                                            ),
                                        shape = MapTestDataHelper.shapeRedC1,
                                    )
                                ),
                        )

                    val otherPatterns =
                        listOf(RoutePatternKey(otherRoute.id, "${otherRoute.id}-pattern"))
                    val routeData =
                        listOfNotNull(
                            routeWithShape(
                                route.id,
                                stops.map { it.id },
                                stops.take(3).associate { it.id to otherPatterns },
                            ),
                            if (otherRouteVisible)
                                routeWithShape(
                                    otherRoute.id,
                                    stops.take(3).map { it.id },
                                    emptyMap(),
                                )
                            else null,
                        )

                    RouteFeaturesBuilder.generateRouteSources(
                            routeData = routeData,
                            globalData = GlobalResponse(objects),
                            alertsByStop = alertsByStop,
                        )
                        .single { it.routeId == route.id }
                        .features
                        .features
                        .map { it.properties[RouteFeaturesBuilder.propAlertStateKey] }
                }

            assertEquals(
                listOf(
                    SegmentAlertState.Normal.name,
                    SegmentAlertState.Suspension.name,
                    SegmentAlertState.Normal.name,
                ),
                alertStatesFor(RouteType.COMMUTER_RAIL, otherRouteVisible = true),
            )
            assertEquals(
                listOf(SegmentAlertState.Suspension.name, SegmentAlertState.Normal.name),
                alertStatesFor(RouteType.COMMUTER_RAIL, otherRouteVisible = false),
            )
            assertEquals(
                listOf(SegmentAlertState.Suspension.name, SegmentAlertState.Normal.name),
                alertStatesFor(RouteType.HEAVY_RAIL, otherRouteVisible = true),
            )
        }

    @Test
    fun `platform stop IDs show the full disruption in both directions unless a visible route serves it`() =
        runBlocking {
            val objects = TestData.clone()
            val route = objects.getRoute("CR-Providence")
            val otherRoute = objects.route { type = RouteType.COMMUTER_RAIL }
            val now = EasternTimeInstant.now()
            objects.alert {
                effect = Alert.Effect.Suspension
                activePeriod(now, null)
                for (direction in listOf(0, 1)) {
                    for (stop in listOf("place-sstat", "NEC-2287")) {
                        informedEntity(
                            listOf(
                                if (direction == 0) Alert.InformedEntity.Activity.Board
                                else Alert.InformedEntity.Activity.Exit,
                                Alert.InformedEntity.Activity.Ride,
                            ),
                            route = route.id.idText,
                            routeType = route.type,
                            stop = stop,
                            directionId = direction,
                        )
                    }
                    for (stop in
                        listOf(
                            "place-bbsta",
                            "NEC-2276",
                            "NEC-2276-01",
                            "NEC-2276-02",
                            "NEC-2276-03",
                        )) {
                        informedEntity(
                            listOf(
                                if (direction == 1) Alert.InformedEntity.Activity.Board
                                else Alert.InformedEntity.Activity.Exit,
                                Alert.InformedEntity.Activity.Ride,
                            ),
                            route = route.id.idText,
                            routeType = route.type,
                            stop = stop,
                            directionId = direction,
                        )
                    }
                }
            }
            val global = GlobalResponse(objects)
            val globalMapData = GlobalMapData(global, AlertsStreamDataResponse(objects), now)
            val shape = Shape("9890004", MapTestDataHelper.shapeRedC1.polyline!!)

            for (direction in listOf(0, 1)) {
                val platformStopIds =
                    if (direction == 0)
                        listOf(
                            "NEC-2287",
                            "NEC-2276-01",
                            "NEC-2265-01",
                            "NEC-2173-01",
                            "SB-0150-04",
                            "SB-0156-S",
                            "SB-0189-S",
                        )
                    else
                        listOf(
                            "SB-0189-S",
                            "SB-0156-S",
                            "SB-0150-06",
                            "NEC-2173-02",
                            "NEC-2265-02",
                            "NEC-2276-02",
                            "NEC-2287",
                        )
                val patternId =
                    if (direction == 0) "CR-Providence-9515a09b-0" else "CR-Providence-6cae46be-1"
                val sharedPattern = RoutePatternKey(otherRoute.id, "other-pattern")
                val segment =
                    RouteSegment(
                        id = "segment",
                        sourceRoutePatternId = patternId,
                        sourceRouteId = route.id,
                        stopIds = platformStopIds,
                        otherPatternsByStopId =
                            mapOf(
                                "NEC-2287" to listOf(sharedPattern),
                                "NEC-2276-01" to listOf(sharedPattern),
                                "NEC-2276-02" to listOf(sharedPattern),
                                "place-bbsta" to
                                    listOf(RoutePatternKey(route.id, "alternate-pattern")),
                            ),
                    )
                val routeShape =
                    SegmentedRouteShape(patternId, route.id, direction, listOf(segment), shape)
                for (otherRouteVisible in listOf(false, true)) {
                    val routeData =
                        listOfNotNull(
                            MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                                route.id,
                                listOf(routeShape),
                            ),
                            if (otherRouteVisible)
                                MapFriendlyRouteResponse.RouteWithSegmentedShapes(
                                    otherRoute.id,
                                    listOf(
                                        routeShape.copy(
                                            sourceRouteId = otherRoute.id,
                                            routeSegments =
                                                listOf(
                                                    segment.copy(
                                                        sourceRouteId = otherRoute.id,
                                                        stopIds =
                                                            listOf("place-sstat", "place-bbsta"),
                                                        otherPatternsByStopId = emptyMap(),
                                                    )
                                                ),
                                        )
                                    ),
                                )
                            else null,
                        )
                    val source =
                        RouteFeaturesBuilder.generateRouteSources(routeData, global, globalMapData)
                            .single { it.routeId == route.id }
                    val suspendedLines =
                        source.lines.filter { it.alertState == SegmentAlertState.Suspension }

                    if (otherRouteVisible) {
                        assertTrue(suspendedLines.isEmpty())
                    } else {
                        val suspended = suspendedLines.single()
                        val expectedStops =
                            if (direction == 0) listOf("place-sstat", "place-bbsta")
                            else listOf("place-bbsta", "place-sstat")
                        assertEquals(expectedStops, suspended.stopIds)
                        assertEquals(
                            LineString(Polyline.decode(shape.polyline!!))
                                .slice(
                                    global.stops.getValue(expectedStops.first()).position,
                                    global.stops.getValue(expectedStops.last()).position,
                                ),
                            suspended.line,
                        )
                    }
                    assertEquals(
                        StopAlertState.Suspension,
                        globalMapData.mapStops
                            .getValue("place-sstat")
                            .alerts
                            ?.get(MapStopRoute.COMMUTER),
                    )
                    assertEquals(
                        StopAlertState.Suspension,
                        globalMapData.mapStops
                            .getValue("place-bbsta")
                            .alerts
                            ?.get(MapStopRoute.COMMUTER),
                    )
                }
            }
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

    @Test
    fun `filter keeps typical shapes if no upcoming trips`() {
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
                listOf(
                    RouteCardData(
                        LineOrRoute.Route(MapTestDataHelper.routeRed),
                        listOf(
                            RouteCardData.RouteStopData(
                                MapTestDataHelper.routeRed,
                                MapTestDataHelper.stopAlewife,
                                listOf(
                                    RouteCardData.Leaf(
                                        LineOrRoute.Route(MapTestDataHelper.routeRed),
                                        MapTestDataHelper.stopAlewife,
                                        Direction(
                                            MapTestDataHelper.patternRed10.directionId,
                                            MapTestDataHelper.routeRed,
                                        ),
                                        listOf(
                                            MapTestDataHelper.patternRed10,
                                            MapTestDataHelper.patternRed30,
                                        ),
                                        setOf(MapTestDataHelper.stopAlewife.id),
                                        emptyList(),
                                        emptyList(),
                                        true,
                                        true,
                                        null,
                                        emptyList(),
                                        RouteCardData.Context.StopDetailsFiltered,
                                    )
                                ),
                            )
                        ),
                        EasternTimeInstant.now(),
                    )
                ),
            )
        assertEquals(filteredShapes.count(), 1)

        val glLineOrRoute =
            LineOrRoute.Line(
                GreenLineTestHelper.line,
                setOf(
                    GreenLineTestHelper.routeB,
                    GreenLineTestHelper.routeC,
                    GreenLineTestHelper.objects.getRoute("Green-D"),
                    GreenLineTestHelper.routeE,
                ),
            )
        val glFilteredShapes =
            RouteFeaturesBuilder.filteredRouteShapesForStop(
                GreenLineTestHelper.stopMapResponse,
                StopDetailsFilter(Line.Id("line-Green"), 0),
                listOf(
                    RouteCardData(
                        glLineOrRoute,
                        listOf(
                            RouteCardData.RouteStopData(
                                glLineOrRoute,
                                GreenLineTestHelper.stopArlington,
                                listOf(
                                    RouteCardData.Leaf(
                                        glLineOrRoute,
                                        GreenLineTestHelper.stopArlington,
                                        Direction(
                                            GreenLineTestHelper.rpB0.directionId,
                                            GreenLineTestHelper.routeB,
                                        ),
                                        listOf(
                                            GreenLineTestHelper.rpB0,
                                            GreenLineTestHelper.rpC0,
                                            GreenLineTestHelper.rpE0,
                                        ),
                                        setOf(GreenLineTestHelper.stopArlington.id),
                                        emptyList(),
                                        emptyList(),
                                        true,
                                        true,
                                        null,
                                        emptyList(),
                                        RouteCardData.Context.StopDetailsFiltered,
                                    )
                                ),
                            )
                        ),
                        EasternTimeInstant.now(),
                    )
                ),
            )
        assertEquals(glFilteredShapes.count(), 3)
        assertEquals(glFilteredShapes[0].segmentedShapes.count(), 1)
    }
}
