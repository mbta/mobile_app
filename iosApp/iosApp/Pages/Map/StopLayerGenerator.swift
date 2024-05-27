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

    static let stopZoomThreshold: Double = 13.0
    static let closeZoomThreshold: Double = 16.0

    static let stopLayerId = "stop-layer"
    static func getTransferLayerId(_ index: Int) -> String {
        "\(stopLayerId)-transfer-\(index.description)"
    }

    static let selectedSizeExpression: Expression = Exp(.switchCase) {
        Exp(.eq) {
            Exp(.get) { StopSourceGenerator.propIsSelectedKey }
            true
        }
        1.25
        1
    }

    static func createStopLayers() -> [SymbolLayer] {
        let sourceId = StopSourceGenerator.stopSourceId
        var stopLayer = SymbolLayer(id: Self.stopLayerId, source: sourceId)
        stopLayer.iconImage = StopIcons.getStopLayerIcon()
        includeSharedProps(on: &stopLayer)

        let transferLayers = (0 ..< 3).map { index in
            var transferLayer = SymbolLayer(id: Self.getTransferLayerId(index), source: sourceId)
            transferLayer.iconImage = StopIcons.getTransferLayerIcon(index)
            transferLayer.iconOffset = getTransferOffsetValue(index: index)
            includeSharedProps(on: &transferLayer)

            return transferLayer
        }

        return [stopLayer] + transferLayers
    }

    static func getTransferOffsetExpression(closeZoom: Bool, _ index: Int) -> Expression {
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

    static func getTransferOffsetValue(index: Int) -> Value<[Double]> {
        .expression(Exp(.step) {
            Exp(.zoom)
            getTransferOffsetExpression(closeZoom: false, index)
            closeZoomThreshold
            getTransferOffsetExpression(closeZoom: true, index)
        })
    }

    static func includeSharedProps(on layer: inout SymbolLayer) {
        // TODO: We actually want to give the icon a halo, but that is only supported for SDFs,
        // which can only be one color.
        // Alternates of stop icon SVGs with halo applied?
        layer.iconSize = .expression(selectedSizeExpression)

        layer.iconAllowOverlap = .constant(true)
        layer.iconOpacity = .constant(0)
        layer.iconOpacityTransition = StyleTransition(duration: 1, delay: 0)
        layer.minZoom = stopZoomThreshold - 1
        layer.symbolSortKey = .expression(Exp(.get) { StopSourceGenerator.propSortOrderKey })
        layer.textAllowOverlap = .constant(true)
    }

    static func xyExp(_ pair: [Double]) -> Expression {
        Exp(.array) { "number"; 2; pair }
    }
}
