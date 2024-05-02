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
    var routeSourceGenerator: RouteSourceGenerator?
    var routeLayerGenerator: RouteLayerGenerator?
    var stopSourceGenerator: StopSourceGenerator?
    var stopLayerGenerator: StopLayerGenerator?

    static let stopLayerTypes: [LocationType] = [.stop, .station]

    init(map: MapboxMap) {
        self.map = map

        for iconId in StopIcons.all {
            do {
                try map.addImage(UIImage(named: iconId)!, id: iconId)
            } catch {
                Logger().error("Failed to add map stop icon image \(iconId)")
            }
        }
    }

    func addSources(routeSourceGenerator: RouteSourceGenerator, stopSourceGenerator: StopSourceGenerator) {
        self.routeSourceGenerator = routeSourceGenerator
        self.stopSourceGenerator = stopSourceGenerator

        addSource(source: routeSourceGenerator.routeSource)

        for source in stopSourceGenerator.stopSources {
            addSource(source: source)
        }
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

    func updateSourceData(routeSourceGenerator: RouteSourceGenerator) {
        self.routeSourceGenerator = routeSourceGenerator
        let routeSource = routeSourceGenerator.routeSource

        if map.sourceExists(withId: routeSource.id) {
            guard let actualData = routeSource.data else { return }
            map.updateGeoJSONSource(withId: routeSource.id, data: actualData)
        } else {
            addSource(source: routeSource)
        }
    }

    func updateSourceData(stopSourceGenerator: StopSourceGenerator) {
        self.stopSourceGenerator = stopSourceGenerator

        stopSourceGenerator.stopSources.forEach { stopSource in
            if map.sourceExists(withId: stopSource.id) {
                guard let actualData = stopSource.data else { return }
                map.updateGeoJSONSource(withId: stopSource.id, data: actualData)
            } else {
                addSource(source: stopSource)
            }
        }
    }

    func updateSourceData(routeSourceGenerator: RouteSourceGenerator, stopSourceGenerator: StopSourceGenerator) {
        updateSourceData(routeSourceGenerator: routeSourceGenerator)
        updateSourceData(stopSourceGenerator: stopSourceGenerator)
    }

    func updateStopLayerZoom(_ zoomLevel: CGFloat) {
        let opacity = zoomLevel > StopIcons.stopZoomThreshold ? 1.0 : 0.0
        for layerType: LocationType in Self.stopLayerTypes {
            let layerId = StopLayerGenerator.getStopLayerId(layerType)
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
