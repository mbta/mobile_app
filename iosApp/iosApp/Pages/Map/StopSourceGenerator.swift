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
    let routes: [Route]
    let feature: Feature
}

class StopSourceGenerator {
    let stops: [String: Stop]
    let selectedStop: Stop?
    let routeLines: [RouteLineData]?
    let alertsByStop: [String: AlertAssociatedStop]
    let routesByStop: [String: [Route]]

    var stopSources: [GeoJSONSource] = []

    private var stopFeatures: [StopFeatureData] = []
    private var touchedStopIds: Set<String> = []

    static let stopSourceId = "stop-source"
    static func getStopSourceId(_ locationType: LocationType) -> String {
        "\(stopSourceId)-\(locationType.name)"
    }

    static let propIdKey = "id"
    static let propServiceStatusKey = "serviceStatus"
    static let propIsSelectedKey = "isSelected"

    init(
        stops: [String: Stop],
        selectedStop: Stop? = nil,
        routeLines: [RouteLineData]? = nil,
        alertsByStop: [String: AlertAssociatedStop] = [:],
        routesByStop: [String: [Route]] = [:]
    ) {
        self.stops = stops
        self.selectedStop = selectedStop
        self.routeLines = routeLines
        self.alertsByStop = alertsByStop
        self.routesByStop = routesByStop

        stopFeatures = generateRouteAssociatedStops() + generateRemainingStops()
        stopSources = generateStopSources()
    }

    func generateRouteAssociatedStops() -> [StopFeatureData] {
        guard let routeLines else { return [] }
        return routeLines.flatMap { lineData in
            lineData.stopIds.compactMap { childStopId in
                guard let stopOnRoute = stops[childStopId] else {
                    return nil
                }
                let stop = stopOnRoute.resolveParent(stops: stops)

                if touchedStopIds.contains(stop.id) { return nil }

                let snappedCoord = lineData.line.closestCoordinate(to: stop.coordinate)?.coordinate
                touchedStopIds.insert(stop.id)
                return .init(
                    stop: stop,
                    routes: routesByStop[stop.id] ?? [],
                    feature: generateStopFeature(stop, routesByStop[stop.id] ?? [], at: snappedCoord)
                )
            }
        }
    }

    func generateRemainingStops() -> [StopFeatureData] {
        stops.values.compactMap { (stop: Stop) -> StopFeatureData? in
            if touchedStopIds.contains(stop.id) { return nil }
            if stop.parentStationId != nil { return nil }

            touchedStopIds.insert(stop.id)
            return .init(stop: stop, routes: [], feature: generateStopFeature(stop))
        }
    }

    func generateStopFeature(
        _ stop: Stop,
        _ routes: [Route] = [],
        at overrideLocation: CLLocationCoordinate2D? = nil
    ) -> Feature {
        var stopFeature = Feature(geometry: Point(overrideLocation ?? stop.coordinate))
        stopFeature.identifier = FeatureIdentifier(stop.id)
        stopFeature.properties = generateStopFeatureProperties(stop, routes)
        return stopFeature
    }

    func generateStopFeatureProperties(_ stop: Stop, _ routes: [Route] = []) -> JSONObject {
        var featureProps = JSONObject()
        featureProps[Self.propIdKey] = JSONValue(String(describing: stop.id))
        featureProps[Self.propServiceStatusKey] = JSONValue(String(describing: getServiceStatus(stop: stop)))
        featureProps[Self.propIsSelectedKey] = JSONValue(stop.id == selectedStop?.id)
        featureProps["routeCount"] = JSONValue(integerLiteral: routes.count)
        featureProps["routes"] = JSONValue.array(JSONArray(routes.map { JSONValue($0.id.starts(with: "Green") ? "Green" : $0.id) }))
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
