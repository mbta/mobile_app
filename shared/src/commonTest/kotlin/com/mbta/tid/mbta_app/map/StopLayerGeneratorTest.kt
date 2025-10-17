package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.map.style.evaluate
import com.mbta.tid.mbta_app.model.GlobalMapData
import com.mbta.tid.mbta_app.model.MapStop
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopAlertState
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class StopLayerGeneratorTest {
    @Test
    fun `stop layers are created`() = runBlocking {
        val stopLayers =
            StopLayerGenerator.createStopLayers(ColorPalette.light, StopLayerGenerator.State())

        assertEquals(11, stopLayers.size)
        assertEquals(StopLayerGenerator.stopTouchTargetLayerId, stopLayers[0].id)
        assertEquals(StopLayerGenerator.busLayerId, stopLayers[1].id)
        assertEquals(StopLayerGenerator.busAlertLayerId, stopLayers[2].id)
        assertEquals(StopLayerGenerator.stopLayerId, stopLayers[3].id)
        assertEquals(StopLayerGenerator.getTransferLayerId(0), stopLayers[4].id)
        assertEquals(StopLayerGenerator.getTransferLayerId(1), stopLayers[5].id)
        assertEquals(StopLayerGenerator.getTransferLayerId(2), stopLayers[6].id)
        assertEquals(StopLayerGenerator.getAlertLayerId(0), stopLayers[7].id)
        assertEquals(StopLayerGenerator.getAlertLayerId(1), stopLayers[8].id)
        assertEquals(StopLayerGenerator.getAlertLayerId(2), stopLayers[9].id)
        assertEquals(StopLayerGenerator.stopLayerSelectedPinId, stopLayers[10].id)
    }

    @Test
    fun `North Station with alerts has correct properties`() = runBlocking {
        val northStation = TestData.getStop("place-north")
        val routes =
            mapOf(
                MapStopRoute.ORANGE to listOf(TestData.getRoute("Orange")),
                MapStopRoute.GREEN to
                    listOf(TestData.getRoute("Green-D"), TestData.getRoute("Green-E")),
                MapStopRoute.COMMUTER to
                    listOf(
                        TestData.getRoute("CR-Fitchburg"),
                        TestData.getRoute("CR-Haverhill"),
                        TestData.getRoute("CR-Lowell"),
                        TestData.getRoute("CR-Newburyport"),
                    ),
            )
        val routeDirections =
            mapOf(
                Route.Id("Orange") to setOf(0, 1),
                Route.Id("Green-D") to setOf(0, 1),
                Route.Id("Green-E") to setOf(0, 1),
                Route.Id("CR-Fitchburg") to setOf(0, 1),
                Route.Id("CR-Haverhill") to setOf(0, 1),
                Route.Id("CR-Lowell") to setOf(0, 1),
                Route.Id("CR-Newburyport") to setOf(0, 1),
            )

        val mapStops =
            mapOf(
                northStation.id to
                    MapStop(
                        stop = northStation,
                        routes = routes,
                        routeTypes =
                            listOf(MapStopRoute.ORANGE, MapStopRoute.GREEN, MapStopRoute.COMMUTER),
                        routeDirections = routeDirections,
                        isTerminal = true,
                        alerts =
                            mapOf(
                                MapStopRoute.ORANGE to StopAlertState.Shuttle,
                                MapStopRoute.GREEN to StopAlertState.Suspension,
                                MapStopRoute.COMMUTER to StopAlertState.Normal,
                                MapStopRoute.BUS to StopAlertState.Normal,
                            ),
                    )
            )

        val feature = StopFeaturesBuilder.buildCollection(mapStops, emptyList()).features.single()

        val stopLayers =
            StopLayerGenerator.createStopLayers(ColorPalette.light, StopLayerGenerator.State())

        val busLayer = stopLayers[1]
        val busAlertLayer = stopLayers[2]
        val stopLayer = stopLayers[3]
        val transferLayer0 = stopLayers[4]
        val transferLayer1 = stopLayers[5]
        val transferLayer2 = stopLayers[6]
        val alertLayer0 = stopLayers[7]
        val alertLayer1 = stopLayers[8]
        val alertLayer2 = stopLayers[9]
        val selectedPinLayer = stopLayers[10]

        assertEquals("", busLayer.iconImage!!.evaluate(feature.properties, zoom = 14.0))
        assertEquals("", busLayer.textField!!.evaluate(feature.properties, zoom = 14.0))
        assertEquals("", busAlertLayer.iconImage!!.evaluate(feature.properties, zoom = 14.0))

        assertEquals(
            "map-stop-container-wide-3",
            stopLayer.iconImage!!.evaluate(feature.properties, zoom = 14.0),
        )
        assertEquals(
            "North Station",
            stopLayer.textField!!.evaluate(feature.properties, zoom = 14.0),
        )

        assertEquals(
            "map-stop-wide-ORANGE",
            transferLayer0.iconImage!!.evaluate(feature.properties, zoom = 14.0),
        )
        assertEquals(
            "map-stop-wide-GREEN",
            transferLayer1.iconImage!!.evaluate(feature.properties, zoom = 14.0),
        )
        assertEquals(
            "map-stop-wide-COMMUTER",
            transferLayer2.iconImage!!.evaluate(feature.properties, zoom = 14.0),
        )

        assertEquals(
            "alert-small-orange-shuttle",
            alertLayer0.iconImage!!.evaluate(feature.properties, zoom = 14.0),
        )
        assertEquals(
            "alert-small-green-suspension",
            alertLayer1.iconImage!!.evaluate(feature.properties, zoom = 14.0),
        )
        assertEquals("", alertLayer2.iconImage!!.evaluate(feature.properties, zoom = 14.0))
        assertEquals("", selectedPinLayer.iconImage!!.evaluate(feature.properties, zoom = 14.0))
    }

    @Test
    fun `state selected stop applies correctly`() = runBlocking {
        val mapStops =
            mapOf(
                MapTestDataHelper.stopAlewife.id to MapTestDataHelper.mapStopAlewife,
                MapTestDataHelper.stopAssembly.id to MapTestDataHelper.mapStopAssembly,
            )
        val features = StopFeaturesBuilder.buildCollection(mapStops, emptyList()).features

        val alewifeFeature = features.first { it.id == MapTestDataHelper.stopAlewife.id }
        val assemblyFeature = features.first { it.id == MapTestDataHelper.stopAssembly.id }

        val stopLayers =
            StopLayerGenerator.createStopLayers(
                ColorPalette.light,
                StopLayerGenerator.State(selectedStopId = alewifeFeature.id),
            )

        val stopLayer = stopLayers[3]
        val selectedPinLayer = stopLayers[10]

        assertTrue(stopLayer.filter!!.evaluate(alewifeFeature.properties, zoom = 2.0))
        assertFalse(stopLayer.filter!!.evaluate(assemblyFeature.properties, zoom = 2.0))

        assertEquals(
            StopIcons.stopPinIcon,
            selectedPinLayer.iconImage!!.evaluate(alewifeFeature.properties, zoom = 14.0),
        )
        assertEquals(
            "",
            selectedPinLayer.iconImage!!.evaluate(assemblyFeature.properties, zoom = 14.0),
        )
    }

    @Test
    fun `state stop details filter applies correctly`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val subwayRoute =
            objects.route {
                id = "Orange"
                type = RouteType.HEAVY_RAIL
            }
        val selectedBusRoute = objects.route { type = RouteType.BUS }
        val otherBusRoute = objects.route { type = RouteType.BUS }
        val busAndSubwayStop = objects.stop()
        val wrongRouteBusStop = objects.stop()
        val wrongDirectionBusStop = objects.stop()
        val rightBusStop = objects.stop()

        objects.routePattern(subwayRoute) {
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { stopIds = listOf(busAndSubwayStop.id) }
        }
        objects.routePattern(selectedBusRoute) {
            typicality = RoutePattern.Typicality.Typical
            directionId = 0
            representativeTrip { stopIds = listOf(rightBusStop.id) }
        }
        objects.routePattern(selectedBusRoute) {
            typicality = RoutePattern.Typicality.Typical
            directionId = 1
            representativeTrip { stopIds = listOf(wrongDirectionBusStop.id) }
        }
        objects.routePattern(otherBusRoute) {
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { stopIds = listOf(busAndSubwayStop.id, wrongRouteBusStop.id) }
        }

        val globalMapData = GlobalMapData(GlobalResponse(objects), emptyMap())
        val features = StopFeaturesBuilder.buildCollection(globalMapData, emptyList()).features
        val busAndSubwayStopFeature = features.first { it.id == busAndSubwayStop.id }
        val wrongRouteBusStopFeature = features.first { it.id == wrongRouteBusStop.id }
        val wrongDirectionBusStopFeature = features.first { it.id == wrongDirectionBusStop.id }
        val rightBusStopFeature = features.first { it.id == rightBusStop.id }

        val stopLayers =
            StopLayerGenerator.createStopLayers(
                ColorPalette.light,
                StopLayerGenerator.State(
                    stopFilter = StopDetailsFilter(routeId = selectedBusRoute.id, directionId = 0)
                ),
            )
        val stopLayer = stopLayers.first { it.id == StopLayerGenerator.stopLayerId }

        assertEquals(
            true,
            stopLayer.filter!!.evaluate(busAndSubwayStopFeature.properties, zoom = 13.0),
        )
        assertEquals(
            false,
            stopLayer.filter!!.evaluate(wrongRouteBusStopFeature.properties, zoom = 13.0),
        )
        assertEquals(
            true,
            stopLayer.filter!!.evaluate(wrongRouteBusStopFeature.properties, zoom = 16.0),
        )
        assertEquals(
            false,
            stopLayer.filter!!.evaluate(wrongDirectionBusStopFeature.properties, zoom = 13.0),
        )
        assertEquals(
            true,
            stopLayer.filter!!.evaluate(wrongDirectionBusStopFeature.properties, zoom = 16.0),
        )
        assertEquals(true, stopLayer.filter!!.evaluate(rightBusStopFeature.properties, zoom = 13.0))
    }
}
