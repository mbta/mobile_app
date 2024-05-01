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

class MapLayerManager: IMapLayerManager {
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
                    print("adding layer \(layer.id)")
                    try map.addLayer(layer, layerPosition: .below("puck"))
                } else {
                    print("adding layer \(layer.id)")
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

    func setVisibleLayers(routeSourceGenerator: RouteSourceGenerator, routesById: [String: Route]) {
        let oldRouteIds: Set<String> = Set(routeIdsFromSources(sources: map.allSourceIdentifiers))
        let newRouteIds: Set<String> = Set(routeSourceGenerator.routeSourceDetails.map(\.routeId))

        let routeIdsToRemove = oldRouteIds.subtracting(newRouteIds)

        for routeId in routeIdsToRemove {
            setLayerVisibility(routeId: routeId, visible: false)
        }

        // TODO: Stop storing these?
        self.routeSourceGenerator = routeSourceGenerator

        routeSourceGenerator.routeSourceDetails.forEach { routeSourceDetails in
            let sourceId = routeSourceDetails.source.id
            let routeId = routeSourceDetails.routeId
            if map.sourceExists(withId: sourceId) {
                guard let actualData = routeSourceDetails.source.data else { return }
                map.updateGeoJSONSource(withId: sourceId, data: actualData)
                setLayerVisibility(routeId: routeId, visible: true)

            } else {
                addSource(source: routeSourceDetails.source)
                let newLayers = RouteLayerGenerator.createAllRouteLayers(routeIds: [routeSourceDetails.routeId], routesById: routesById)
                for layer in newLayers {
                    do {
                        try map.addLayer(layer)
                    } catch {
                        Sentry.shared.captureError(error: error)
                    }
                }
            }
        }
    }

    func routeIdsFromSources(sources: [SourceInfo]) -> [String] {
        let prefixSize = RouteSourceGenerator.routeSourceId.count + 1 // +1 for joining dash
        return sources.filter { source in
            source.id.hasPrefix(RouteSourceGenerator.routeSourceId)
        }.map { routeSource in
            String(routeSource.id.dropFirst(prefixSize))
        }
    }

    func setLayerVisibility(routeId: String, visible: Bool) {
        let visibility: Value<MapboxMaps.Visibility> = visible ? .constant(.visible) : .constant(.none)

        let baseRouteLayerId = RouteLayerGenerator.getRouteLayerId(routeId)
        let allRouteLayerIds = [baseRouteLayerId, "\(baseRouteLayerId)-alerting", "\(baseRouteLayerId)-alerting-bg"]

        for layerId in allRouteLayerIds {
            do {
                try map.updateLayer(withId: layerId, type: LineLayer.self) { layer in
                    layer.visibility = visibility
                }
            } catch {
                Sentry.shared.captureError(error: error)
            }
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
