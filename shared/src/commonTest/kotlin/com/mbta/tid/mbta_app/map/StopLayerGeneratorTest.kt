package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.map.style.evaluate
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.MapStop
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopAlertState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class StopLayerGeneratorTest {
    @Test
    fun `stop layers are created`() = runBlocking {
        val stopLayers = StopLayerGenerator.createStopLayers(ColorPalette.light)

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
        val northStation =
            Stop(
                id = "place-north",
                latitude = 42.365577,
                longitude = -71.06129,
                name = "North Station",
                locationType = LocationType.STATION,
                description = null,
                platformName = null,
                childStopIds =
                    listOf(
                        "70026",
                        "70027",
                        "70205",
                        "70206",
                        "BNT-0000",
                        "BNT-0000-01",
                        "BNT-0000-02",
                        "BNT-0000-03",
                        "BNT-0000-04",
                        "BNT-0000-05",
                        "BNT-0000-06",
                        "BNT-0000-07",
                        "BNT-0000-08",
                        "BNT-0000-09",
                        "BNT-0000-10",
                        "BNT-0000-B1",
                        "BNT-0000-B2",
                        "door-north-causewaye",
                        "door-north-causeways",
                        "door-north-crcanal",
                        "door-north-crcauseway",
                        "door-north-crnashua",
                        "door-north-tdgarden",
                        "door-north-valenti"
                    ),
                connectingStopIds = listOf("9070026", "114", "113"),
                parentStationId = null
            )
        val routes =
            mapOf(
                MapStopRoute.ORANGE to
                    listOf(
                        Route(
                            id = "Orange",
                            type = RouteType.HEAVY_RAIL,
                            color = "ED8B00",
                            directionNames = listOf("South", "North"),
                            directionDestinations = listOf("Forest Hills", "Oak Grove"),
                            longName = "Orange Line",
                            shortName = "",
                            sortOrder = 10020,
                            textColor = "FFFFFF",
                            lineId = "line-Orange",
                            routePatternIds = null
                        )
                    ),
                MapStopRoute.GREEN to
                    listOf(
                        Route(
                            id = "Green-D",
                            type = RouteType.LIGHT_RAIL,
                            color = "00843D",
                            directionNames = listOf("West", "East"),
                            directionDestinations = listOf("Riverside", "Union Square"),
                            longName = "Green Line D",
                            shortName = "D",
                            sortOrder = 10034,
                            textColor = "FFFFFF",
                            lineId = "line-Green",
                            routePatternIds = null
                        ),
                        Route(
                            id = "Green-E",
                            type = RouteType.LIGHT_RAIL,
                            color = "00843D",
                            directionNames = listOf("West", "East"),
                            directionDestinations = listOf("Heath Street", "Medford/Tufts"),
                            longName = "Green Line E",
                            shortName = "E",
                            sortOrder = 10035,
                            textColor = "FFFFFF",
                            lineId = "line-Green",
                            routePatternIds = null
                        )
                    ),
                MapStopRoute.COMMUTER to
                    listOf(
                        Route(
                            id = "CR-Fitchburg",
                            type = RouteType.COMMUTER_RAIL,
                            color = "80276C",
                            directionNames = listOf("Outbound", "Inbound"),
                            directionDestinations = listOf("Wachusett", "North Station"),
                            longName = "Fitchburg Line",
                            shortName = "",
                            sortOrder = 20002,
                            textColor = "FFFFFF",
                            lineId = "line-Fitchburg",
                            routePatternIds = null
                        ),
                        Route(
                            id = "CR-Haverhill",
                            type = RouteType.COMMUTER_RAIL,
                            color = "80276C",
                            directionNames = listOf("Outbound", "Inbound"),
                            directionDestinations = listOf("Haverhill", "North Station"),
                            longName = "Haverhill Line",
                            shortName = "",
                            sortOrder = 20006,
                            textColor = "FFFFFF",
                            lineId = "line-Haverhill",
                            routePatternIds = null
                        ),
                        Route(
                            id = "CR-Lowell",
                            type = RouteType.COMMUTER_RAIL,
                            color = "80276C",
                            directionNames = listOf("Outbound", "Inbound"),
                            directionDestinations = listOf("Lowell", "North Station"),
                            longName = "Lowell Line",
                            shortName = "",
                            sortOrder = 20008,
                            textColor = "FFFFFF",
                            lineId = "line-Lowell",
                            routePatternIds = null
                        ),
                        Route(
                            id = "CR-Newburyport",
                            type = RouteType.COMMUTER_RAIL,
                            color = "80276C",
                            directionNames = listOf("Outbound", "Inbound"),
                            directionDestinations =
                                listOf("Newburyport or Rockport", "North Station"),
                            longName = "Newburyport/Rockport Line",
                            shortName = "",
                            sortOrder = 20011,
                            textColor = "FFFFFF",
                            lineId = "line-Newburyport",
                            routePatternIds = null
                        )
                    )
            )

        val mapStops =
            mapOf(
                northStation.id to
                    MapStop(
                        stop = northStation,
                        routes = routes,
                        routeTypes =
                            listOf(MapStopRoute.ORANGE, MapStopRoute.GREEN, MapStopRoute.COMMUTER),
                        isTerminal = true,
                        alerts =
                            mapOf(
                                MapStopRoute.ORANGE to StopAlertState.Shuttle,
                                MapStopRoute.GREEN to StopAlertState.Suspension,
                                MapStopRoute.COMMUTER to StopAlertState.Normal,
                                MapStopRoute.BUS to StopAlertState.Normal
                            )
                    )
            )

        val feature =
            StopFeaturesBuilder.buildCollection(StopSourceData(), mapStops, emptyList())
                .features
                .single()

        val stopLayers = StopLayerGenerator.createStopLayers(ColorPalette.light)

        val busLayer = stopLayers[1]
        val busAlertLayer = stopLayers[2]
        val stopLayer = stopLayers[3]
        val transferLayer0 = stopLayers[4]
        val transferLayer1 = stopLayers[5]
        val transferLayer2 = stopLayers[6]
        val alertLayer0 = stopLayers[7]
        val alertLayer1 = stopLayers[8]
        val alertLayer2 = stopLayers[9]

        assertEquals("", busLayer.iconImage!!.evaluate(feature.properties, zoom = 14.0))
        assertEquals("", busLayer.textField!!.evaluate(feature.properties, zoom = 14.0))
        assertEquals("", busAlertLayer.iconImage!!.evaluate(feature.properties, zoom = 14.0))

        assertEquals(
            "map-stop-container-wide-3",
            stopLayer.iconImage!!.evaluate(feature.properties, zoom = 14.0)
        )
        assertEquals(
            "North Station",
            stopLayer.textField!!.evaluate(feature.properties, zoom = 14.0)
        )

        assertEquals(
            "map-stop-wide-ORANGE",
            transferLayer0.iconImage!!.evaluate(feature.properties, zoom = 14.0)
        )
        assertEquals(
            "map-stop-wide-GREEN",
            transferLayer1.iconImage!!.evaluate(feature.properties, zoom = 14.0)
        )
        assertEquals(
            "map-stop-wide-COMMUTER",
            transferLayer2.iconImage!!.evaluate(feature.properties, zoom = 14.0)
        )

        assertEquals(
            "alert-small-orange-shuttle",
            alertLayer0.iconImage!!.evaluate(feature.properties, zoom = 14.0)
        )
        assertEquals(
            "alert-small-green-suspension",
            alertLayer1.iconImage!!.evaluate(feature.properties, zoom = 14.0)
        )
        assertEquals("", alertLayer2.iconImage!!.evaluate(feature.properties, zoom = 14.0))
    }
}
