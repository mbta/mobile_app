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
    var currentScheme: ColorScheme? { get }
    func addIcons(recreate: Bool)
    func addLayers(colorScheme: ColorScheme, recreate: Bool)
    func updateVisibleLayers(colorScheme: ColorScheme)
    func updateSourceData(routeData: MapboxMaps.FeatureCollection)
    func updateSourceData(stopData: MapboxMaps.FeatureCollection)
}

struct MapImageError: Error {}

class MapLayerManager: IMapLayerManager {
    var currentScheme: ColorScheme?
    let map: MapboxMap

    init(map: MapboxMap) {
        self.map = map
        addIcons()
    }

    func addIcons(recreate: Bool = false) {
        for iconId in StopIcons.shared.all + AlertIcons.shared.all {
            do {
                guard let image = UIImage(named: iconId) else { throw MapImageError() }
                if map.imageExists(withId: iconId) {
                    if recreate {
                        try map.removeImage(withId: iconId)
                    } else {
                        continue
                    }
                }
                try map.addImage(image, id: iconId)
            } catch {
                Logger().error("Failed to add map icon image \(iconId)\n\(error)")
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

    func addLayers(colorScheme: ColorScheme, recreate: Bool = false) {
        let colorPalette = getColorPalette(colorScheme: colorScheme)
        currentScheme = colorScheme
        let layers = generateLayers(colorScheme: colorScheme)

        for layer in layers {
            do {
                if map.layerExists(withId: layer.id) {
                    if recreate {
                        try map.removeLayer(withId: layer.id)
                    } else {
                        // Skip attempting to add layer if it already exists
                        continue
                    }
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

    func updateVisibleLayers(colorScheme: ColorScheme) {
        let newLayers = generateLayers(colorScheme: colorScheme)
        let newLayerIds = newLayers.map(\.id)
        for layerIdentifier in map.allLayerIdentifiers {
            do {
                if layerIdentifier.id.contains(StopLayerGenerator.shared.stopLayerId),
                   !newLayerIds.contains(layerIdentifier.id) {
                    try map.removeLayer(withId: layerIdentifier.id)
                }
            } catch {
                Logger().error("Failed to remove layer \(layerIdentifier.id)\n\(error)")
            }
        }

        for layer in newLayers {
            do {
                if !map.allLayerIdentifiers.contains(where: { layerInfo in layerInfo.id == layer.id }) {
                    try map.addLayer(layer)
                }
            } catch {
                Logger().error("Failed to add layer \(layer.id)\n\(error)")
            }
        }
    }

    func getColorPalette(colorScheme: ColorScheme) -> ColorPalette {
        switch colorScheme {
        case .light: ColorPalette.companion.light
        case .dark: ColorPalette.companion.dark
        @unknown default: ColorPalette.companion.light
        }
    }

    func generateLayers(colorScheme: ColorScheme) -> [MapboxMaps.Layer] {
        let colorPalette = getColorPalette(colorScheme: colorScheme)
        return RouteLayerGenerator.shared.createAllRouteLayers(colorPalette: colorPalette)
            .map { $0.toMapbox() } + StopLayerGenerator.shared.createStopLayers(
                colorPalette: colorPalette,
                zoom: Float(map.cameraState.zoom)
            ).map { $0.toMapbox()
            }
    }

    func updateSourceData(sourceId: String, data: MapboxMaps.FeatureCollection) {
        if map.sourceExists(withId: sourceId) {
            map.updateGeoJSONSource(withId: sourceId, data: .featureCollection(data))
        } else {
            var source = GeoJSONSource(id: sourceId)
            source.data = .featureCollection(data)
            addSource(source: source)
        }
    }

    func updateSourceData(routeData: MapboxMaps.FeatureCollection) {
        updateSourceData(sourceId: RouteFeaturesBuilder.shared.routeSourceId, data: routeData)
    }

    func updateSourceData(stopData: MapboxMaps.FeatureCollection) {
        updateSourceData(sourceId: StopFeaturesBuilder.shared.stopSourceId, data: stopData)
    }
}
