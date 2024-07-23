//
//  StopLayerGenerator.swift
//  iosApp
//
//  Created by Simon, Emma on 3/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI
@_spi(Experimental) import MapboxMaps

class StopLayerGenerator {
    let stopLayers: [SymbolLayer] = createStopLayers()

    static let maxTransferLayers = 3
    static let stopZoomThreshold = 8.0

    static let stopLayerId = "stop-layer"
    static let stopTouchTargetLayerId = "\(stopLayerId)-touch-target"
    static let stopLayerSelectedPinId = "\(stopLayerId)-selected-pin"

    static let busLayerId = "\(stopLayerId)-bus"
    static let busAlertLayerId = "\(stopLayerId)-bus-alert"

    static func getAlertLayerId(_ index: Int) -> String {
        "\(stopLayerId)-alert-\(index.description)"
    }

    static func getTransferLayerId(_ index: Int) -> String {
        "\(stopLayerId)-transfer-\(index.description)"
    }

    static func createStopLayers() -> [SymbolLayer] {
        let sourceId = StopFeaturesBuilder.shared.stopSourceId
        let stopLayer = createStopLayer(id: Self.stopLayerId)
        let busLayer = createStopLayer(id: Self.busLayerId, forBus: true)

        var stopTouchTargetLayer = SymbolLayer(id: Self.stopTouchTargetLayerId, source: sourceId)
        stopTouchTargetLayer.iconImage = .expression(Exp(.image) { StopIcons.stopDummyIcon })
        stopTouchTargetLayer.iconPadding = .constant(22.0)
        includeSharedProps(on: &stopTouchTargetLayer)

        var stopSelectedPinLayer = SymbolLayer(id: Self.stopLayerSelectedPinId, source: sourceId)
        stopSelectedPinLayer.iconImage = .expression(Exp(.switchCase) {
            MapExp.shared.selectedExp.toMapbox()
            Exp(.image) { StopIcons.stopPinIcon }
            Exp(.image) { "" }
        })
        stopSelectedPinLayer.iconOffset = offsetPinValue()
        includeSharedProps(on: &stopSelectedPinLayer)

        let transferLayers = (0 ..< Self.maxTransferLayers).map { index in
            var transferLayer = SymbolLayer(id: Self.getTransferLayerId(index), source: sourceId)
            transferLayer.iconImage = StopIcons.getTransferLayerIcon(index)
            transferLayer.iconOffset = offsetTransferValue(index: index)
            includeSharedProps(on: &transferLayer)

            return transferLayer
        }

        let alertLayers = (0 ..< Self.maxTransferLayers).map { index in
            createAlertLayer(id: Self.getAlertLayerId(index), index: index)
        }

        let busAlertLayer = createAlertLayer(id: busAlertLayerId, forBus: true)

        return [stopTouchTargetLayer, busLayer, busAlertLayer, stopLayer] + transferLayers +
            alertLayers + [stopSelectedPinLayer]
    }

    static func createAlertLayer(id: String, index: Int = 0, forBus: Bool = false) -> SymbolLayer {
        var alertLayer = SymbolLayer(id: id, source: StopFeaturesBuilder.shared.stopSourceId)
        alertLayer.iconImage = AlertIcons.getAlertLayerIcon(index, forBus: forBus)
        alertLayer.iconOffset = offsetAlertValue(index: index)
        alertLayer.iconAllowOverlap = .constant(true)
        includeSharedProps(on: &alertLayer)

        return alertLayer
    }

    static func createStopLayer(id: String, forBus: Bool = false) -> SymbolLayer {
        var stopLayer = SymbolLayer(id: id, source: StopFeaturesBuilder.shared.stopSourceId)
        stopLayer.iconImage = StopIcons.getStopLayerIcon(forBus: forBus)
        stopLayer.textField = .expression(MapExp.shared.stopLabelTextExp(forBus: forBus))

        stopLayer.textColor = .constant(.init(.text))
        stopLayer.textFont = .constant(["Inter Regular"])
        stopLayer.textHaloColor = .constant(.init(.fill3))
        stopLayer.textHaloWidth = .constant(2.0)
        stopLayer.textSize = .constant(13)
        stopLayer.textVariableAnchor = .constant([.right, .bottom, .top, .left])
        stopLayer.textJustify = .constant(.auto)
        stopLayer.textAllowOverlap = .constant(true)
        stopLayer.textOptional = .constant(true)
        stopLayer.textOffset = .expression(MapExp.shared.labelOffsetExp.toMapbox())

        includeSharedProps(on: &stopLayer)
        return stopLayer
    }

    static func includeSharedProps(on layer: inout SymbolLayer) {
        layer.iconSize = .expression(MapExp.shared.selectedSizeExp.toMapbox())

        layer.iconAllowOverlap = .constant(true)
        layer.minZoom = stopZoomThreshold
        layer.symbolSortKey = .expression(Exp(.get) { StopFeaturesBuilder.shared.propSortOrderKey })
    }

    static func offsetAlertValue(index: Int) -> Value<[Double]> {
        .expression(Exp(.step) {
            Exp(.zoom)
            MapExp.shared.offsetAlertExp(closeZoom: false, index: Int32(index)).toMapbox()
            MapDefaults.shared.closeZoomThreshold
            MapExp.shared.offsetAlertExp(closeZoom: true, index: Int32(index)).toMapbox()
        })
    }

    static func offsetTransferValue(index: Int) -> Value<[Double]> {
        .expression(Exp(.step) {
            Exp(.zoom)
            MapExp.shared.offsetTransferExp(closeZoom: false, index: Int32(index)).toMapbox()
            MapDefaults.shared.closeZoomThreshold
            MapExp.shared.offsetTransferExp(closeZoom: true, index: Int32(index)).toMapbox()
        })
    }

    static func offsetPinValue() -> Value<[Double]> {
        .expression(Exp(.step) {
            Exp(.zoom)
            MapExp.shared.offsetPinExp(closeZoom: false).toMapbox()
            MapDefaults.shared.closeZoomThreshold
            MapExp.shared.offsetPinExp(closeZoom: true).toMapbox()
        })
    }
}
