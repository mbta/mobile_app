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

protocol IMapLayerManager {
    var routeSourceGenerator: RouteSourceGenerator? { get }
    var routeLayerGenerator: RouteLayerGenerator? { get }
    var stopSourceGenerator: StopSourceGenerator? { get }
    var stopLayerGenerator: StopLayerGenerator? { get }

    func addSources(routeSourceGenerator: RouteSourceGenerator, stopSourceGenerator: StopSourceGenerator)
    func addLayers(routeLayerGenerator: RouteLayerGenerator, stopLayerGenerator: StopLayerGenerator)
    func updateSourceData(routeSourceGenerator: RouteSourceGenerator, stopSourceGenerator: StopSourceGenerator)
    func updateSourceData(routeSourceGenerator: RouteSourceGenerator)
    func updateSourceData(stopSourceGenerator: StopSourceGenerator)
    func updateStopLayerZoom(_ zoomLevel: CGFloat)
}

struct MapImageError: Error {}

class MapLayerManager: IMapLayerManager {
    let map: MapboxMap
    var routeSourceGenerator: RouteSourceGenerator?
    var routeLayerGenerator: RouteLayerGenerator?
    var stopSourceGenerator: StopSourceGenerator?
    var stopLayerGenerator: StopLayerGenerator?

    init(map: MapboxMap) {
        self.map = map

        for iconId in StopIcons.all + AlertIcons.all {
            do {
                guard let image = UIImage(named: iconId) else { throw MapImageError() }
                try map.addImage(image, id: iconId)
            } catch {
                Logger().error("Failed to add map icon image \(iconId)")
            }
        }
    }

    func addSources(routeSourceGenerator: RouteSourceGenerator, stopSourceGenerator: StopSourceGenerator) {
        self.routeSourceGenerator = routeSourceGenerator
        self.stopSourceGenerator = stopSourceGenerator

        updateSourceData(source: routeSourceGenerator.routeSource)
        updateSourceData(source: stopSourceGenerator.stopSource)
    }

    private func addSource(source: GeoJSONSource) {
        do {
            try map.addSource(source)
        } catch {
            Logger().error("Failed to add source \(source.id)\n\(error)")
        }
    }

    func addLayers(routeLayerGenerator: RouteLayerGenerator, stopLayerGenerator: StopLayerGenerator) {
        self.routeLayerGenerator = routeLayerGenerator
        self.stopLayerGenerator = stopLayerGenerator

        let layers: [Layer] = routeLayerGenerator.routeLayers + stopLayerGenerator.stopLayers
        for layer in layers {
            do {
                if map.layerExists(withId: layer.id) {
                    // Skip attempting to add layer if it already exists
                    continue
                }
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

    func updateSourceData(source: GeoJSONSource) {
        if map.sourceExists(withId: source.id) {
            guard let actualData = source.data else { return }
            map.updateGeoJSONSource(withId: source.id, data: actualData)
        } else {
            addSource(source: source)
        }
    }

    func updateSourceData(routeSourceGenerator: RouteSourceGenerator) {
        self.routeSourceGenerator = routeSourceGenerator
        updateSourceData(source: routeSourceGenerator.routeSource)
    }

    func updateSourceData(stopSourceGenerator: StopSourceGenerator) {
        self.stopSourceGenerator = stopSourceGenerator
        updateSourceData(source: stopSourceGenerator.stopSource)
    }

    func updateSourceData(routeSourceGenerator: RouteSourceGenerator, stopSourceGenerator: StopSourceGenerator) {
        updateSourceData(routeSourceGenerator: routeSourceGenerator)
        updateSourceData(stopSourceGenerator: stopSourceGenerator)
    }

    func updateStopLayerZoom(_ zoomLevel: CGFloat) {
        let opacity = zoomLevel > StopLayerGenerator.stopZoomThreshold ? 1.0 : 0.0
        for layerId in (0 ..< StopLayerGenerator.maxTransferLayers).flatMap({
            [StopLayerGenerator.getTransferLayerId($0), StopLayerGenerator.getAlertLayerId($0)]
        }) + [StopLayerGenerator.stopLayerId] {
            do {
                try map.updateLayer(withId: layerId, type: SymbolLayer.self) { layer in
                    if layer.iconOpacity != .constant(opacity) {
                        layer.iconOpacity = .constant(opacity)
                    }
                }
            } catch {
                Logger().error("Failed to update layer \(layerId)\n\(error)")
            }
        }
    }
}
