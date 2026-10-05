package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.map.style.ArrayType
import com.mbta.tid.mbta_app.map.style.Exp
import com.mbta.tid.mbta_app.map.style.Interpolation
import com.mbta.tid.mbta_app.map.style.SymbolLayer
import com.mbta.tid.mbta_app.map.style.TextAnchor
import com.mbta.tid.mbta_app.map.style.TextJustify
import com.mbta.tid.mbta_app.map.style.downcastToColor
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.repositories.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi

public object StopLayerGenerator {
    internal val maxTransferLayers = 3
    public val stopZoomThreshold: Double = 11.0
    internal val busStopZoomThreshold = 12.0

    public val stopLayerId: String = "stop-layer"
    public val stopTouchTargetLayerId: String = "${stopLayerId}-touch-target"
    internal val stopLayerSelectedPinId = "${stopLayerId}-selected-pin"

    public val busLayerId: String = "${stopLayerId}-bus"
    internal val busAlertLayerId = "${stopLayerId}-bus-alert"

    internal fun getAlertLayerId(index: Int) = "${stopLayerId}-alert-${index}"

    internal fun getTransferLayerId(index: Int) = "${stopLayerId}-transfer-${index}"

    public sealed class State {
        public data object Default : State()

        public data class StopDetails(val selectedStopId: String) : State()

        public data class TripDetails(
            val selectedStopId: String?,
            val stopFilter: StopDetailsFilter?,
            val tripStops: List<String>?,
        ) : State()
    }

    public suspend fun createStopLayers(
        colorPalette: ColorPalette,
        state: State,
        settings: Map<Settings, Boolean>,
    ): List<SymbolLayer> {
        return withContext(Dispatchers.Default) {
            val sourceId = StopFeaturesBuilder.stopSourceId

            val stopLayer =
                createStopLayer(
                    id = stopLayerId,
                    colorPalette = colorPalette,
                    state = state,
                    settings = settings,
                )

            val stopTouchTargetLayer = SymbolLayer(id = stopTouchTargetLayerId, source = sourceId)
            stopTouchTargetLayer.iconImage = Exp.image(Exp(StopIcons.stopDummyIcon))
            stopTouchTargetLayer.iconPadding = 22.0
            includeSharedProps(stopTouchTargetLayer, forBus = false, state = state)
            if (settings[Settings.ShiftingIncludeStops] == true) {
                stopTouchTargetLayer.iconOffset = shiftFromLine(settings, scale = 1.0)
            }

            val stopSelectedPinLayer = SymbolLayer(id = stopLayerSelectedPinId, source = sourceId)
            stopSelectedPinLayer.iconImage =
                Exp.case(
                    MapExp.selectedExp(state) to Exp.image(Exp(StopIcons.stopPinIcon)),
                    Exp.image(Exp("")),
                )
            stopSelectedPinLayer.textField =
                Exp.case(
                    MapExp.selectedExp(state) to Exp.get(StopFeaturesBuilder.propNameKey),
                    Exp(""),
                )
            includedDefaultTextProps(stopSelectedPinLayer, colorPalette, settings)
            stopSelectedPinLayer.iconOffset = offsetPinValue(settings)
            stopTouchTargetLayer.filter =
                Exp.ge(
                    Exp.zoom(),
                    Exp.case(MapExp.isBusExp to Exp(busStopZoomThreshold), Exp(stopZoomThreshold)),
                )
            includeSharedProps(stopSelectedPinLayer, forBus = false, state = state)

            val transferLayers =
                (0.rangeUntil(maxTransferLayers)).map { index ->
                    val transferLayer =
                        SymbolLayer(id = getTransferLayerId(index), source = sourceId)
                    transferLayer.iconImage = (StopIcons.getTransferLayerIcon(index))
                    transferLayer.iconOffset = offsetTransferValue(index, settings)
                    includeSharedProps(transferLayer, forBus = false, state = state)

                    return@map transferLayer
                }

            val alertLayers =
                (0.rangeUntil(maxTransferLayers)).map { index ->
                    createAlertLayer(
                        id = getAlertLayerId(index),
                        index,
                        state = state,
                        settings = settings,
                    )
                }

            val busLayer =
                createStopLayer(
                    id = busLayerId,
                    forBus = true,
                    colorPalette,
                    state = state,
                    settings,
                )
            val busAlertLayer =
                createAlertLayer(
                    id = busAlertLayerId,
                    forBus = true,
                    state = state,
                    settings = settings,
                )

            listOf(stopTouchTargetLayer, busLayer, busAlertLayer, stopLayer) +
                transferLayers +
                alertLayers +
                listOf(stopSelectedPinLayer)
        }
    }

    private fun shiftFromLine(
        settings: Map<Settings, Boolean>,
        scale: Double = 1.0,
    ): Exp<List<Number>> {
        val routeIds = Exp.get(StopFeaturesBuilder.propRouteIdsKey)
        val mapRoutes = Exp.get(StopFeaturesBuilder.propMapRoutesKey)
        fun b(relativeScale: Double = 1.0) =
            Exp.array(
                ArrayType.Number,
                2,
                listOf(
                    MapExp.lineShiftEast(scale * relativeScale, routeIds, mapRoutes, settings),
                    Exp(0),
                ),
            )
        return if (settings[Settings.ShiftingScaleWithZoom] == true) {
            Exp.interpolate(
                Interpolation.Linear,
                Exp.zoom(),
                Exp(6) to b(0.5),
                Exp(MapDefaults.midZoomThreshold) to b(0.5),
                Exp(MapDefaults.closeZoomThreshold) to b(1.0),
            )
        } else {
            b(1.0)
        }
    }

    internal fun createAlertLayer(
        id: String,
        index: Int = 0,
        forBus: Boolean = false,
        state: State,
        settings: Map<Settings, Boolean>,
    ): SymbolLayer {
        val alertLayer = SymbolLayer(id = id, source = StopFeaturesBuilder.stopSourceId)
        alertLayer.iconImage = AlertIcons.getAlertLayerIcon(index, forBus = forBus)
        alertLayer.iconOffset = offsetAlertValue(index, settings)
        alertLayer.iconAllowOverlap = true
        includeSharedProps(alertLayer, forBus, state)

        return alertLayer
    }

    internal fun createStopLayer(
        id: String,
        forBus: Boolean = false,
        colorPalette: ColorPalette,
        state: State,
        settings: Map<Settings, Boolean>,
    ): SymbolLayer {
        val stopLayer = SymbolLayer(id = id, source = StopFeaturesBuilder.stopSourceId)
        stopLayer.iconImage = (StopIcons.getStopLayerIcon(forBus = forBus))
        stopLayer.textField = (MapExp.stopLabelTextExp(forBus = forBus, state = state))
        includedDefaultTextProps(stopLayer, colorPalette, settings)
        stopLayer.textAllowOverlap = false

        includeSharedProps(stopLayer, forBus, state)
        if (settings[Settings.ShiftingIncludeStops] == true) {
            stopLayer.iconOffset =
                shiftFromLine(settings, scale = 1.0) // TODO figure out why 1/24 doesn’t work
        }
        return stopLayer
    }

    internal fun includedDefaultTextProps(
        layer: SymbolLayer,
        colorPalette: ColorPalette,
        settings: Map<Settings, Boolean>,
    ) {
        layer.textColor = Exp(colorPalette.text).downcastToColor()
        layer.textFont = listOf("Inter Regular")
        layer.textHaloColor = Exp(colorPalette.fill3).downcastToColor()
        layer.textHaloWidth = 2.0
        layer.textSize = 13.0
        layer.textVariableAnchor =
            listOf(TextAnchor.RIGHT, TextAnchor.BOTTOM, TextAnchor.TOP, TextAnchor.LEFT)
        layer.textJustify = TextJustify.AUTO
        layer.textAllowOverlap = true
        layer.textOptional = true
        layer.textOffset = MapExp.labelOffsetExp(settings)
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal fun includeSharedProps(layer: SymbolLayer, forBus: Boolean, state: State) {
        layer.iconSize = MapExp.selectedSizeExp(state)

        layer.iconAllowOverlap = true
        layer.symbolSortKey = Exp.get(StopFeaturesBuilder.propSortOrderKey)
        layer.filter =
            if (state is State.TripDetails)
                Exp.all(
                    selectedOrBeyondThresholdExp(state, forBus),
                    // we hide bus stops off this trip/route at zoom levels < 15, so we show
                    // non-bus stops, trip/route stops, or zoom > 15
                    Exp.any(
                        MapExp.listNotEq(
                            Exp.get(StopFeaturesBuilder.propMapRoutesKey),
                            listOf(Exp(MapStopRoute.BUS.name)),
                        ),
                        if (!state.tripStops.isNullOrEmpty())
                            Exp.`in`(
                                Exp.get(StopFeaturesBuilder.propIdKey),
                                Exp.array(contents = state.tripStops.map(::Exp)),
                            )
                        else if (state.stopFilter != null)
                            Exp.`in`(
                                Exp("${state.stopFilter.routeId}/${state.stopFilter.directionId}"),
                                Exp.get(StopFeaturesBuilder.propAllRouteDirectionsKey),
                            )
                        else Exp(false),
                        Exp.ge(Exp.zoom(), Exp(MapDefaults.closeZoomThreshold)),
                    ),
                )
            else selectedOrBeyondThresholdExp(state, forBus)
    }

    private fun selectedOrBeyondThresholdExp(state: State, forBus: Boolean) =
        Exp.any(
            MapExp.selectedExp(state),
            Exp.ge(Exp.zoom(), if (forBus) Exp(busStopZoomThreshold) else Exp(stopZoomThreshold)),
        )

    internal fun offsetAlertValue(index: Int, settings: Map<Settings, Boolean>): Exp<List<Number>> {
        return Exp.step(
            Exp.zoom(),
            MapExp.offsetAlertExp(closeZoom = false, index, settings),
            Exp(MapDefaults.closeZoomThreshold) to
                MapExp.offsetAlertExp(closeZoom = true, index, settings),
        )
    }

    internal fun offsetTransferValue(
        index: Int,
        settings: Map<Settings, Boolean>,
    ): Exp<List<Number>> {
        return Exp.step(
            Exp.zoom(),
            MapExp.offsetTransferExp(closeZoom = false, index, settings),
            Exp(MapDefaults.closeZoomThreshold) to
                MapExp.offsetTransferExp(closeZoom = true, index, settings),
        )
    }

    internal fun offsetPinValue(settings: Map<Settings, Boolean>): Exp<List<Number>> {
        return Exp.step(
            Exp.zoom(),
            MapExp.offsetPinExp(closeZoom = false, settings = settings),
            Exp(MapDefaults.closeZoomThreshold) to
                MapExp.offsetPinExp(closeZoom = true, settings = settings),
        )
    }
}
