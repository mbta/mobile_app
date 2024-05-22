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
    let stopLayerTypes: [LocationType]
    let stopLayers: [SymbolLayer]

    static let stopLayerId = "stop-layer"
    static func getStopLayerId(_ locationType: LocationType) -> String {
        "\(stopLayerId)-\(locationType.name)"
    }

    init(stopLayerTypes: [LocationType]) {
        self.stopLayerTypes = stopLayerTypes
        stopLayers = Self.createStopLayers(stopLayerTypes: stopLayerTypes)
    }

    static func createStopLayers(stopLayerTypes: [LocationType]) -> [SymbolLayer] {
        stopLayerTypes.map { Self.createStopLayer(locationType: $0) }
            + createStopTransferLayers()
    }

    static func createStopLayer(locationType: LocationType) -> SymbolLayer {
        let layerId = Self.getStopLayerId(locationType)
        let sourceId = StopSourceGenerator.getStopSourceId(locationType)
        var stopLayer = SymbolLayer(id: layerId, source: sourceId)
        stopLayer.iconImage = StopIcons.getStopLayerIcon(locationType)

        // TODO: We actually want to give the icon a halo, but that is only supported for SDFs,
        // which can only be one color.
        // Alternates of stop icon SVGs with halo applied?
        stopLayer.iconSize = .expression(Exp(.switchCase) {
            Exp(.eq) {
                Exp(.get) { StopSourceGenerator.propIsSelectedKey }
                true
            }
            1.25
            1
        }
        )

        stopLayer.iconAllowOverlap = .constant(true)
        stopLayer.minZoom = StopIcons.stopZoomThreshold - 1
        stopLayer.iconOpacity = .constant(0)
        stopLayer.iconOpacityTransition = StyleTransition(duration: 1, delay: 0)

        return stopLayer
    }

    static func createStopTransferLayers() -> [SymbolLayer] {
        let sourceId = StopSourceGenerator.getStopSourceId(.station)

        var transferLayer1 = SymbolLayer(id: "stopTransfers1", source: sourceId)
        transferLayer1.iconImage = .expression(Exp(.step) {
            Exp(.get) { "routeCount" }
            ""
            2
            Exp(.concat) {
                "map-stop-pill-"
                Exp(.at) { 0; Exp(.array) { Exp(.get) { "routes" } }}
            }
        })
        transferLayer1.iconOffset = .constant([0, -12.5])
        transferLayer1.iconAllowOverlap = .constant(true)

        var transferLayer2 = SymbolLayer(id: "stopTransfers2", source: sourceId)
        transferLayer2.iconImage = .expression(Exp(.step) {
            Exp(.get) { "routeCount" }
            ""
            2
            Exp(.concat) {
                "map-stop-pill-"
                Exp(.at) { 1; Exp(.array) { Exp(.get) { "routes" } } }
            }
        })
        transferLayer2.iconOffset = .constant([0, 12.5])
        transferLayer2.iconAllowOverlap = .constant(true)

        return [transferLayer1, transferLayer2]
    }
}
