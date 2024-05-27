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

    static let stopZoomThreshold = 8.0
    static let midZoomThreshold = 11.0
    static let closeZoomThreshold = 16.0

    static let stopLayerId = "stop-layer"
    static func getTransferLayerId(_ index: Int) -> String {
        "\(stopLayerId)-transfer-\(index.description)"
    }

    static let routesExp = Exp(.get) { StopSourceGenerator.propMapRoutesKey }

    static let selectedExp = Exp(.boolean) { Exp(.get) { StopSourceGenerator.propIsSelectedKey } }
    static let selectedSizeExp: Expression =
        Exp(.interpolate) {
            Exp(.exponential) { 2 }
            Exp(.zoom)
            midZoomThreshold; withMultipliers(0.2, modeResize: [0.5, 3, 2])
            12; withMultipliers(0.5, modeResize: [0.75, 2, 2])
            14; withMultipliers(1)
        }

    static let topRouteExp = Exp(.string) {
        Exp(.switchCase) {
            Exp(.eq) { Exp(.length) { routesExp }; 0 }
            ""
            Exp(.at) { 0; routesExp }
        }
    }

    static func createStopLayers() -> [SymbolLayer] {
        let sourceId = StopSourceGenerator.stopSourceId
        var stopLayer = SymbolLayer(id: Self.stopLayerId, source: sourceId)
        stopLayer.iconImage = StopIcons.getStopLayerIcon()
        includeSharedProps(on: &stopLayer)

        let transferLayers = (0 ..< 3).map { index in
            var transferLayer = SymbolLayer(id: Self.getTransferLayerId(index), source: sourceId)
            transferLayer.iconImage = StopIcons.getTransferLayerIcon(index)
            transferLayer.iconOffset = transferOffsetValue(index: index)
            includeSharedProps(on: &transferLayer)

            return transferLayer
        }

        return [stopLayer] + transferLayers
    }

    static func includeSharedProps(on layer: inout SymbolLayer) {
        layer.iconSize = .expression(selectedSizeExp)

        layer.iconAllowOverlap = .constant(true)
        layer.iconOpacity = .constant(0)
        layer.iconOpacityTransition = StyleTransition(duration: 1, delay: 0)
        layer.minZoom = stopZoomThreshold - 1
        layer.symbolSortKey = .expression(Exp(.get) { StopSourceGenerator.propSortOrderKey })
        layer.textAllowOverlap = .constant(true)
    }

    static func modeSizeMultiplierExp(resizeWith: [Double]) -> Expression {
        Exp(.switchCase) {
            Exp(.eq) { topRouteExp; MapStopRoute.bus.name }
            resizeWith[0]
            Exp(.eq) { topRouteExp; MapStopRoute.commuter.name }
            resizeWith[1]
            resizeWith[2]
        }
    }

    static func transferOffsetExp(closeZoom: Bool, _ index: Int) -> Expression {
        let doubleRouteOffset: Double = closeZoom ? 13 : 8
        let tripleRouteOffset: Double = closeZoom ? 26 : 16
        return Exp(.step) {
            Exp(.length) { Exp(.get) { StopSourceGenerator.propMapRoutesKey } }
            xyExp([0, 0])
            2
            xyExp([[0, -doubleRouteOffset], [0, doubleRouteOffset], [0, 0]][index])
            3
            xyExp([[0, -tripleRouteOffset], [0, 0], [0, tripleRouteOffset]][index])
        }
    }

    static func transferOffsetValue(index: Int) -> Value<[Double]> {
        .expression(Exp(.step) {
            Exp(.zoom)
            transferOffsetExp(closeZoom: false, index)
            closeZoomThreshold
            transferOffsetExp(closeZoom: true, index)
        })
    }

    // The modeResize array must contain 3 entries for [BUS, COMMUTER, fallback]
    static func withMultipliers(_ base: Double, modeResize: [Double] = [1, 1, 1]) -> Expression {
        Exp(.product) {
            base
            modeSizeMultiplierExp(resizeWith: modeResize)
            // TODO: We actually want to give the icon a halo rather than resize,
            // but that is only supported for SDFs, which can only be one color.
            // Alternates of stop icon SVGs with halo applied?
            Exp(.switchCase) { selectedExp; 1.25; 1 }
        }
    }

    static func xyExp(_ pair: [Double]) -> Expression {
        Exp(.array) { "number"; 2; pair }
    }
}
