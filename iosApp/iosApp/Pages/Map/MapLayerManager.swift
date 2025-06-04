//
//  MapLayerManager.swift
//  iosApp
//
//  Created by Simon, Emma on 3/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@_spi(Experimental) import MapboxMaps
import os
import Shared
import SwiftUI

protocol IMapLayerManager {
    var currentScheme: ColorScheme? { get }
    func addIcons(recreate: Bool)
    func addLayers(
        routes: [MapFriendlyRouteResponse.RouteWithSegmentedShapes],
        state: StopLayerGenerator.State,
        globalResponse: GlobalResponse,
        colorScheme: ColorScheme
    )
    func resetPuckPosition()
    func updateSourceData(routeData: [RouteSourceData])
    func updateSourceData(stopData: MapboxMaps.FeatureCollection)
}

extension IMapLayerManager {
    func addLayers(
        mapFriendlyRouteResponse: MapFriendlyRouteResponse,
        state: StopLayerGenerator.State,
        globalResponse: GlobalResponse,
        colorScheme: ColorScheme
    ) {
        addLayers(
            routes: mapFriendlyRouteResponse.routesWithSegmentedShapes,
            state: state,
            globalResponse: globalResponse,
            colorScheme: colorScheme
        )
    }
}

struct MapImageError: Error {}

class MapLayerManager: IMapLayerManager {
    var currentScheme: ColorScheme?
    let map: MapboxMap

    private static let bufferLayerId = "empty-layer-between-routes-and-stops"

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
     Adds persistent layers so that they are persisted even if the underlying map style changes.

     https://docs.mapbox.com/ios/maps/api/11.5.0/documentation/mapboxmaps/stylemanager/addpersistentlayer(_:layerposition:)
     */
    func addLayers(
        routes: [MapFriendlyRouteResponse.RouteWithSegmentedShapes],
        state: StopLayerGenerator.State,
        globalResponse: GlobalResponse,
        colorScheme: ColorScheme
    ) {
        Task {
            let colorPalette = getColorPalette(colorScheme: colorScheme)
            currentScheme = colorScheme
            let routeLayers = try await RouteLayerGenerator.shared.createAllRouteLayers(
                routesWithShapes: routes,
                globalResponse: globalResponse,
                colorPalette: colorPalette
            )
            .map { $0.toMapbox() }
            let stopLayers = try await StopLayerGenerator.shared.createStopLayers(
                colorPalette: colorPalette,
                state: state
            )
            .map { $0.toMapbox() }

            await setLayers(routeLayers: routeLayers, stopLayers: stopLayers)
        }
    }

    @MainActor private func setLayers(
        routeLayers: [MapboxMaps.Layer],
        stopLayers: [MapboxMaps.Layer]
    ) {
        if !map.layerExists(withId: Self.bufferLayerId) {
            let layer = SlotLayer(id: Self.bufferLayerId)
            if map.layerExists(withId: "puck") {
                try? map.addPersistentLayer(layer, layerPosition: .below("puck"))
            } else {
                try? map.addPersistentLayer(layer)
            }
        }
        var oldLayers = Set(map.allLayerIdentifiers.map(\.id))
        for layer in routeLayers {
            do {
                oldLayers.remove(layer.id)
                if map.layerExists(withId: layer.id) {
                    try map.removeLayer(withId: layer.id)
                }

                try map.addPersistentLayer(layer, layerPosition: .below(Self.bufferLayerId))
            } catch {
                Logger().error("Failed to add layer \(layer.id)\n\(error)")
            }
        }
        for layer in stopLayers {
            do {
                oldLayers.remove(layer.id)
                if map.layerExists(withId: layer.id) {
                    try map.removeLayer(withId: layer.id)
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
        for oldLayer in oldLayers {
            if oldLayer.starts(with: RouteLayerGenerator.shared.routeLayerId) {
                try? map.removeLayer(withId: oldLayer)
            }
        }
        resetPuckPosition()
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

    func updateSourceData(routeData: [RouteSourceData]) {
        for data in routeData {
            updateSourceData(
                sourceId: RouteFeaturesBuilder.shared.getRouteSourceId(routeId: data.routeId),
                data: data.features.toMapbox()
            )
        }
    }

    func updateSourceData(stopData: MapboxMaps.FeatureCollection) {
        updateSourceData(sourceId: StopFeaturesBuilder.shared.stopSourceId, data: stopData)
    }
}
