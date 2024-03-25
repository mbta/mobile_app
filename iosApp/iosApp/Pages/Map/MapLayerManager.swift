//
//  MapLayerManager.swift
//  iosApp
//
//  Created by Simon, Emma on 3/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import os
import shared
import SwiftUI
@_spi(Experimental) import MapboxMaps

class MapLayerManager {
    let map: MapboxMap

    static let stopZoomThreshold: CGFloat = 13.0
    static let tombstoneZoomThreshold: CGFloat = 16.0

    static let stationIconId = "t-station"
    static let stopIconId = "bus-stop"
    static let stopIconSmallId = "bus-stop-small"

    static let stopIcons: [String] = [stationIconId, stopIconId, stopIconSmallId]

    static let stopLayerTypes: [LocationType] = [.stop, .station]

    static func getStopLayerIcon(_ locationType: LocationType, _ zoomLevel: CGFloat) -> Value<ResolvedImage> {
        if locationType == .station { return .constant(.name(stationIconId)) }
        if locationType == .stop, zoomLevel > tombstoneZoomThreshold { return .constant(.name(stopIconId)) }
        return .constant(.name(stopIconSmallId))
    }

    init(map: MapboxMap) {
        self.map = map

        for iconId in Self.stopIcons {
            do {
                try map.addImage(UIImage(named: iconId)!, id: iconId)
            } catch {
                Logger().error("Failed to add map stop icon image \(iconId)")
            }
        }
    }

    func addSources(sources: [GeoJSONSource]) {
        for source in sources {
            do {
                try map.addSource(source)
            } catch {
                Logger().error("Failed to add source \(source.id)\n\(error)")
            }
        }
    }

    func addLayers(layers: [Layer]) {
        for layer in layers {
            do {
                if map.layerExists(withId: "puck") {
                    try map.addLayer(layer, layerPosition: .below("puck"))
                } else {
                    try map.addLayer(layer)
                }
            } catch {
                Logger().error("Failed to add layer \(layer.id)\n\(error)")
            }
        }
    }

    func updateStopLayers(_ zoomLevel: CGFloat) {
        let opacity = zoomLevel > Self.stopZoomThreshold ? 1.0 : 0.0
        for layerType: LocationType in Self.stopLayerTypes {
            let layerId = StopLayerGenerator.getStopLayerId(layerType)
            do {
                try map.updateLayer(withId: layerId, type: SymbolLayer.self) { layer in
                    if layer.iconOpacity != .constant(opacity) {
                        layer.iconOpacity = .constant(opacity)
                    }
                    if layerType == .stop {
                        let icon = Self.getStopLayerIcon(layerType, zoomLevel)
                        if layer.iconImage != icon {
                            layer.iconImage = icon
                        }
                    }
                }
            } catch {
                Logger().error("Failed to update layer \(layerId)\n\(error)")
            }
        }
    }
}
