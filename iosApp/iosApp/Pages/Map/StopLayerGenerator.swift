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

    static let busLayerId = "\(stopLayerId)-bus"
    static let busAlertLayerId = "\(stopLayerId)-bus-alert"

    static func getAlertLayerId(_ index: Int) -> String {
        "\(stopLayerId)-alert-\(index.description)"
    }

    static func getTransferLayerId(_ index: Int) -> String {
        "\(stopLayerId)-transfer-\(index.description)"
    }

    static func createStopLayers() -> [SymbolLayer] {
        let sourceId = StopSourceGenerator.stopSourceId
        let stopLayer = createStopLayer(id: Self.stopLayerId)
        let busLayer = createStopLayer(id: Self.busLayerId, forBus: true)

        var stopTouchTargetLayer = SymbolLayer(id: Self.stopTouchTargetLayerId, source: sourceId)
        stopTouchTargetLayer.iconImage = .expression(Exp(.image) { StopIcons.stopDummyIcon })
        stopTouchTargetLayer.iconPadding = .constant(22.0)
        includeSharedProps(on: &stopTouchTargetLayer)

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

        return [stopTouchTargetLayer, busLayer, busAlertLayer, stopLayer] + transferLayers + alertLayers
    }

    static func createAlertLayer(id: String, index: Int = 0, forBus: Bool = false) -> SymbolLayer {
        var alertLayer = SymbolLayer(id: id, source: StopSourceGenerator.stopSourceId)
        alertLayer.iconImage = AlertIcons.getAlertLayerIcon(index, forBus: forBus)
        alertLayer.iconOffset = offsetAlertValue(index: index)
        alertLayer.iconAllowOverlap = .constant(true)
        includeSharedProps(on: &alertLayer)

        return alertLayer
    }

    static func createStopLayer(id: String, forBus: Bool = false) -> SymbolLayer {
        var stopLayer = SymbolLayer(id: id, source: StopSourceGenerator.stopSourceId)
        stopLayer.iconImage = StopIcons.getStopLayerIcon(forBus: forBus)
        stopLayer.textField = .expression(MapExp.stopLabelTextExp(forBus: forBus))

        stopLayer.textColor = .constant(.init(.text))
        stopLayer.textHaloColor = .constant(.init(.fill3))
        stopLayer.textHaloWidth = .constant(2.0)
        stopLayer.textSize = .constant(13)
        stopLayer.textVariableAnchor = .constant([.right, .bottom, .top, .left])
        stopLayer.textJustify = .constant(.auto)
        stopLayer.textAllowOverlap = .constant(true)
        stopLayer.textOptional = .constant(true)
        stopLayer.textOffset = .expression(MapExp.labelOffsetExp)

        includeSharedProps(on: &stopLayer)
        return stopLayer
    }

    static func includeSharedProps(on layer: inout SymbolLayer) {
        layer.iconSize = .expression(MapExp.selectedSizeExp)

        layer.iconAllowOverlap = .constant(true)
        layer.minZoom = stopZoomThreshold
        layer.symbolSortKey = .expression(Exp(.get) { StopSourceGenerator.propSortOrderKey })
    }

    static func offsetAlertValue(index: Int) -> Value<[Double]> {
        .expression(Exp(.step) {
            Exp(.zoom)
            MapExp.offsetAlertExp(closeZoom: false, index)
            MapDefaults.closeZoomThreshold
            MapExp.offsetAlertExp(closeZoom: true, index)
        })
    }

    static func offsetTransferValue(index: Int) -> Value<[Double]> {
        .expression(Exp(.step) {
            Exp(.zoom)
            MapExp.offsetTransferExp(closeZoom: false, index)
            MapDefaults.closeZoomThreshold
            MapExp.offsetTransferExp(closeZoom: true, index)
        })
    }
}
