//
//  StopSourceGenerator.swift
//  iosApp
//
//  Created by Simon, Emma on 3/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Polyline
import shared
@_spi(Experimental) import MapboxMaps

struct StopFeatureData {
    let stop: Stop
    let feature: Feature
}

class StopSourceGenerator {
    let stops: [String: Stop]
    let routeSourceDetails: [RouteSourceData]?

    var stopSources: [GeoJSONSource] = []

    private var stopFeatures: [StopFeatureData] = []
    private var touchedStopIds: Set<String> = []

    static let stopSourceId = "stop-source"
    static func getStopSourceId(_ locationType: LocationType) -> String {
        "\(stopSourceId)-\(locationType.name)"
    }

    init(stops: [String: Stop], routeSourceDetails: [RouteSourceData]? = nil) {
        self.stops = stops
        self.routeSourceDetails = routeSourceDetails

        stopFeatures = generateStopFeatures()
        stopSources = generateStopSources()
    }

    func generateStopFeatures() -> [StopFeatureData] {
        generateRouteAssociatedStops() + generateRemainingStops()
    }

    func generateRouteAssociatedStops() -> [StopFeatureData] {
        guard let routeSourceDetails else { return [] }
        return routeSourceDetails.flatMap { routeSource in
            routeSource.lines.flatMap { lineData in
                lineData.stopIds.compactMap { childStopId in
                    guard let stopOnRoute = stops[childStopId],
                          let stop = stopOnRoute.resolveParent(stops) else { return nil }

                    if touchedStopIds.contains(stop.id) { return nil }

                    let snappedCoord = lineData.line.closestCoordinate(to: stop.coordinate)
                    var stopFeature = Feature(geometry: Point(snappedCoord?.coordinate ?? stop.coordinate))
                    stopFeature.identifier = FeatureIdentifier(stop.id)

                    touchedStopIds.insert(stop.id)
                    return .init(stop: stop, feature: stopFeature)
                }
            }
        }
    }

    func generateRemainingStops() -> [StopFeatureData] {
        stops.values.compactMap { stop in
            if touchedStopIds.contains(stop.id) { return nil }
            if stop.parentStationId != nil { return nil }

            var stopFeature = Feature(geometry: Point(stop.coordinate))
            stopFeature.identifier = FeatureIdentifier(stop.id)

            touchedStopIds.insert(stop.id)
            return .init(stop: stop, feature: stopFeature)
        }
    }

    func generateStopSources() -> [GeoJSONSource] {
        Dictionary(grouping: stopFeatures, by: { $0.stop.locationType }).map { type, featureData in
            var stopSource = GeoJSONSource(id: Self.getStopSourceId(type))
            stopSource.data = .featureCollection(FeatureCollection(features: featureData.map(\.feature)))
            return stopSource
        }
    }
}
