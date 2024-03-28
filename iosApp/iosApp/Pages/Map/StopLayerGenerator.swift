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
    }

    static func createStopLayer(locationType: LocationType) -> SymbolLayer {
        let layerId = Self.getStopLayerId(locationType)
        let sourceId = StopSourceGenerator.getStopSourceId(locationType)
        var stopLayer = SymbolLayer(id: layerId, source: sourceId)
        stopLayer.iconImage = Self.getStopLayerIcon(locationType)
        stopLayer.iconAllowOverlap = .constant(true)
        stopLayer.minZoom = MapLayerManager.stopZoomThreshold - 1
        stopLayer.iconOpacity = .constant(0)
        stopLayer.iconOpacityTransition = StyleTransition(duration: 1, delay: 0)

        return stopLayer
    }

    static func getStopLayerIcon(_ locationType: LocationType) -> Value<ResolvedImage> {
        switch locationType {
        case .station:
            .constant(.name(MapLayerManager.stationIconId))
        case .stop:
            .expression(Exp(.step) {
                Exp(.zoom)
                MapLayerManager.stopIconSmallId
                MapLayerManager.tombstoneZoomThreshold
                MapLayerManager.stopIconId
            })
        default:
            .constant(.name(""))
        }
    }
}
