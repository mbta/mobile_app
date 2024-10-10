//
//  MapLayerManager.swift
//  iosApp
//
//  Created by Simon, Emma on 3/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import MapboxMaps
import os
import shared
import SwiftUI

protocol IMapLayerManager {
    var currentScheme: ColorScheme? { get }
    func addIcons(recreate: Bool)
    func addLayers(colorScheme: ColorScheme, recreate: Bool)
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

    /*
     Adds persistent layers so that they are persisted even if the underlying map style changes. To intentionally
     re-create the layers due to a change that corresponds with a style change (such as colorScheme changing),
     set recreate to true.

     https://docs.mapbox.com/ios/maps/api/11.5.0/documentation/mapboxmaps/stylemanager/addpersistentlayer(_:layerposition:)
     */
    func addLayers(colorScheme: ColorScheme, recreate: Bool = false) {
        let colorPalette = getColorPalette(colorScheme: colorScheme)
        currentScheme = colorScheme
        let layers: [MapboxMaps.Layer] = RouteLayerGenerator.shared.createAllRouteLayers(colorPalette: colorPalette)
            .map { $0.toMapbox() } + StopLayerGenerator.shared.createStopLayers(
                colorPalette: colorPalette
            ).map { $0.toMapbox() }

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
                    try map.addPersistentLayer(layer, layerPosition: .below("puck"))
                } else {
                    try map.addPersistentLayer(layer)
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
