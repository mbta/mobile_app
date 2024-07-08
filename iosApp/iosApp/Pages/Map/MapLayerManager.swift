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
    var routeLayerGenerator: RouteLayerGenerator? { get }
    var stopLayerGenerator: StopLayerGenerator? { get }

    func addSources(
        childStopSourceGenerator: ChildStopSourceGenerator
    )
    func addLayers(
        routeLayerGenerator: RouteLayerGenerator,
        stopLayerGenerator: StopLayerGenerator,
        childStopLayerGenerator: ChildStopLayerGenerator
    )

    func updateSourceData(routeSource: GeoJSONSource)
    func updateSourceData(stopSource: GeoJSONSource)
    func updateSourceData(childStopSourceGenerator: ChildStopSourceGenerator)
}

struct MapImageError: Error {}

class MapLayerManager: IMapLayerManager {
    let map: MapboxMap
    var routeLayerGenerator: RouteLayerGenerator?
    var stopLayerGenerator: StopLayerGenerator?
    var childStopSourceGenerator: ChildStopSourceGenerator?
    var childStopLayerGenerator: ChildStopLayerGenerator?

    init(map: MapboxMap) {
        self.map = map

        for iconId in StopIcons.all + AlertIcons.all + ChildStopIcons.all {
            do {
                guard let image = UIImage(named: iconId) else { throw MapImageError() }
                try map.addImage(image, id: iconId)
            } catch {
                Logger().error("Failed to add map icon image \(iconId)")
            }
        }
    }

    func addSources(
        childStopSourceGenerator: ChildStopSourceGenerator
    ) {
        self.childStopSourceGenerator = childStopSourceGenerator
        updateSourceData(source: childStopSourceGenerator.childStopSource)
    }

    private func addSource(source: GeoJSONSource) {
        do {
            try map.addSource(source)
        } catch {
            Logger().error("Failed to add source \(source.id)\n\(error)")
        }
    }

    func addLayers(
        routeLayerGenerator: RouteLayerGenerator,
        stopLayerGenerator: StopLayerGenerator,
        childStopLayerGenerator: ChildStopLayerGenerator
    ) {
        self.routeLayerGenerator = routeLayerGenerator
        self.stopLayerGenerator = stopLayerGenerator
        self.childStopLayerGenerator = childStopLayerGenerator

        let layers: [Layer] = routeLayerGenerator.routeLayers + stopLayerGenerator
            .stopLayers + [childStopLayerGenerator.childStopLayer]
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
            guard let actualData = source.data else {
                return
            }
            map.updateGeoJSONSource(withId: source.id, data: actualData)
        } else {
            addSource(source: source)
        }
    }

    func updateSourceData(routeSource: GeoJSONSource) {
        updateSourceData(source: routeSource)
    }

    func updateSourceData(stopSource: GeoJSONSource) {
        updateSourceData(source: stopSource)
    }

    func updateSourceData(
        childStopSourceGenerator: ChildStopSourceGenerator
    ) {
        updateSourceData(source: childStopSourceGenerator.childStopSource)
    }
}
