//
//  MapLayerManager.swift
//  iosApp
//
//  Created by Simon, Emma on 3/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@_spi(Experimental) import MapboxMaps
import os
import shared
import SwiftUI

protocol IMapLayerManager {
    var currentScheme: ColorScheme? { get }
    func addIcons(recreate: Bool)
    func addLayers(colorScheme: ColorScheme, recreate: Bool)
    func resetPuckPosition()
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
            Task {
                guard let image = UIImage(named: iconId) else { throw MapImageError() }
                DispatchQueue.main.async { [weak self] in
                    guard let self else { return }
                    do {
                        if map.imageExists(withId: iconId) {
                            if recreate {
                                try map.removeImage(withId: iconId)
                            } else {
                                return
                            }
                        }
                        try map.addImage(image, id: iconId)
                    } catch {
                        Logger().error("Failed to add map icon image \(iconId)\n\(error)")
                    }
                }
            }
        }
    }

    private func addSource(source: GeoJSONSource) {
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            do {
                try map.addSource(source)
            } catch {
                Logger().error("Failed to add source \(source.id)\n\(error)")
            }
        }
    }

    /*
     Adds persistent layers so that they are persisted even if the underlying map style changes. To intentionally
     re-create the layers due to a change that corresponds with a style change (such as colorScheme changing),
     set recreate to true.

     https://docs.mapbox.com/ios/maps/api/11.5.0/documentation/mapboxmaps/stylemanager/addpersistentlayer(_:layerposition:)
     */
    func addLayers(colorScheme: ColorScheme, recreate: Bool = false) {
        Task {
            let colorPalette = getColorPalette(colorScheme: colorScheme)
            currentScheme = colorScheme
            let layers: [MapboxMaps.Layer] = RouteLayerGenerator.shared.createAllRouteLayers(colorPalette: colorPalette)
                .map { $0.toMapbox() } + StopLayerGenerator.shared.createStopLayers(
                    colorPalette: colorPalette
                ).map { $0.toMapbox() }

            for layer in layers {
                DispatchQueue.main.async { [weak self] in
                    guard let self else { return }
                    do {
                        if map.layerExists(withId: layer.id) {
                            if recreate {
                                try map.removeLayer(withId: layer.id)
                            } else {
                                // Skip attempting to add layer if it already exists
                                return
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
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                resetPuckPosition()
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

    func resetPuckPosition() {
        do {
            if map.layerExists(withId: "puck") {
                try map.moveLayer(withId: "puck", to: .default)
            }
        } catch {
            Logger().error("Failed to set puck as top layer")
        }
    }

    func updateSourceData(sourceId: String, data: MapboxMaps.FeatureCollection) {
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            if map.sourceExists(withId: sourceId) {
                map.updateGeoJSONSource(withId: sourceId, data: .featureCollection(data))
            } else {
                var source = GeoJSONSource(id: sourceId)
                source.data = .featureCollection(data)
                addSource(source: source)
            }
        }
    }

    func updateSourceData(routeData: MapboxMaps.FeatureCollection) {
        updateSourceData(sourceId: RouteFeaturesBuilder.shared.routeSourceId, data: routeData)
    }

    func updateSourceData(stopData: MapboxMaps.FeatureCollection) {
        updateSourceData(sourceId: StopFeaturesBuilder.shared.stopSourceId, data: stopData)
    }
}
