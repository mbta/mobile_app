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
    func addLayers(
        stopLayerGenerator: StopLayerGenerator,
        childStopLayerGenerator: ChildStopLayerGenerator,
        colorScheme: ColorScheme
    )

    func updateSourceData(routeData: FeatureCollection)
    func updateSourceData(stopSource: GeoJSONSource)
    func updateSourceData(childStopSource: GeoJSONSource)
}

struct MapImageError: Error {}

class MapLayerManager: IMapLayerManager {
    let map: MapboxMap
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

    private func addSource(source: GeoJSONSource) {
        do {
            try map.addSource(source)
        } catch {
            Logger().error("Failed to add source \(source.id)\n\(error)")
        }
    }

    func addLayers(
        stopLayerGenerator: StopLayerGenerator,
        childStopLayerGenerator: ChildStopLayerGenerator,
        colorScheme: ColorScheme
    ) {
        let colorPalette = switch colorScheme {
        case .light: ColorPalette.companion.light
        case .dark: ColorPalette.companion.dark
        @unknown default: ColorPalette.companion.light
        }
        let layers: [MapboxMaps.Layer] = RouteLayerGenerator.shared.createAllRouteLayers(colorPalette: colorPalette)
            .map { $0.toMapbox() }
            + stopLayerGenerator.stopLayers + [childStopLayerGenerator.childStopLayer]
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

    func updateSourceData(sourceId: String, data: FeatureCollection) {
        if map.sourceExists(withId: sourceId) {
            map.updateGeoJSONSource(withId: sourceId, data: .featureCollection(data))
        } else {
            var source = GeoJSONSource(id: sourceId)
            source.data = .featureCollection(data)
            addSource(source: source)
        }
    }

    func updateSourceData(routeData: FeatureCollection) {
        updateSourceData(sourceId: RouteFeaturesBuilder.shared.routeSourceId, data: routeData)
    }

    func updateSourceData(stopSource: GeoJSONSource) {
        updateSourceData(source: stopSource)
    }

    func updateSourceData(childStopSource: GeoJSONSource) {
        updateSourceData(source: childStopSource)
    }
}
