//
//  StopSourceGenerator.swift
//  iosApp
//
//  Created by Simon, Emma on 3/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import OSLog
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
    let alertsByStop: [String: AlertAssociatedStop]

    var stopSources: [GeoJSONSource] = []

    private var stopFeatures: [StopFeatureData] = []
    private var touchedStopIds: Set<String> = []

    static let stopSourceId = "stop-source"
    static func getStopSourceId(_ locationType: LocationType) -> String {
        "\(stopSourceId)-\(locationType.name)"
    }

    static let propIdKey = "id"
    static let propServiceStatusKey = "serviceStatus"

    init(
        stops: [String: Stop],
        routeSourceDetails: [RouteSourceData]? = nil,
        alertsByStop: [String: AlertAssociatedStop]? = nil
    ) {
        self.stops = stops
        self.routeSourceDetails = routeSourceDetails
        self.alertsByStop = alertsByStop ?? [:]

        stopFeatures = generateRouteAssociatedStops() + generateRemainingStops()
        stopSources = generateStopSources()
    }

    func generateRouteAssociatedStops() -> [StopFeatureData] {
        guard let routeSourceDetails else { return [] }
        return routeSourceDetails.flatMap { routeSource in

            routeSource.lines.flatMap { lineData in
                lineData.stopIds.compactMap { childStopId in
                    guard let stopOnRoute = stops[childStopId],
                          let stop = stopOnRoute.resolveParent(stops)
                    else {
                        return nil
                    }

                    if touchedStopIds.contains(stop.id) { return nil }

                    let snappedCoord = lineData.line.closestCoordinate(to: stop.coordinate)?.coordinate
                    touchedStopIds.insert(stop.id)
                    return .init(
                        stop: stop,
                        feature: generateStopFeature(stop, at: snappedCoord)
                    )
                }
            }
        }
    }

    func generateRemainingStops() -> [StopFeatureData] {
        stops.values.compactMap { stop in
            if touchedStopIds.contains(stop.id) { return nil }
            if stop.parentStationId != nil { return nil }

            touchedStopIds.insert(stop.id)
            return .init(stop: stop, feature: generateStopFeature(stop))
        }
    }

    func generateStopFeature(_ stop: Stop, at overrideLocation: CLLocationCoordinate2D? = nil) -> Feature {
        var stopFeature = Feature(geometry: Point(overrideLocation ?? stop.coordinate))
        stopFeature.identifier = FeatureIdentifier(stop.id)
        stopFeature.properties = generateStopFeatureProperties(stop)
        return stopFeature
    }

    func generateStopFeatureProperties(_ stop: Stop) -> JSONObject {
        var featureProps = JSONObject()
        featureProps[Self.propIdKey] = JSONValue(String(describing: stop.id))
        featureProps[Self.propServiceStatusKey] = JSONValue(String(describing: getServiceStatus(stop: stop)))
        return featureProps
    }

    func generateStopSources() -> [GeoJSONSource] {
        Dictionary(grouping: stopFeatures, by: { $0.stop.locationType }).map { type, featureData in
            var stopSource = GeoJSONSource(id: Self.getStopSourceId(type))
            stopSource.data = .featureCollection(FeatureCollection(features: featureData.map(\.feature)))
            return stopSource
        }
    }

    func getServiceStatus(stop: Stop) -> StopServiceStatus {
        alertsByStop[stop.id]?.serviceStatus ?? StopServiceStatus.normal
    }
}
