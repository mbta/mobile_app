//
//  StopSourceGenerator.swift
//  iosApp
//
//  Created by Simon, Emma on 3/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
@_spi(Experimental) import MapboxMaps

struct StopFeatureData {
    let stop: MapStop
    let feature: Feature
}

struct StopSourceData: Equatable {
    var filteredStopIds: [String]?
    var selectedStopId: String?
}

enum StopSourceGenerator {
    static let stopSourceId = "stop-source"

    static let propIdKey = "id"
    static let propIsSelectedKey = "isSelected"
    static let propIsTerminalKey = "isTerminal"
    // Map routes is an array of MapStopRoute enum names
    static let propMapRoutesKey = "mapRoutes"
    static let propNameKey = "name"
    // Route IDs are in a map keyed by MapStopRoute enum names, each with a list of IDs
    static let propRouteIdsKey = "routeIds"
    static let propServiceStatusKey = "serviceStatus"
    static let propSortOrderKey = "sortOrder"

    static func generateStopSource(
        stopData: StopSourceData,
        stops: [String: MapStop],
        linesToSnap: [RouteLineData]
    ) -> GeoJSONSource {
        let filteredStops = if let filteredStopIds = stopData.filteredStopIds {
            stops.filter { filteredStopIds.contains($0.key) }
        } else {
            stops
        }
        let stopFeatures = generateStopFeatures(stopData, filteredStops, linesToSnap)
        return generateStopSource(stopFeatures: stopFeatures)
    }

    static func generateStopSource(stopFeatures: [StopFeatureData]) -> GeoJSONSource {
        var stopSource = GeoJSONSource(id: Self.stopSourceId)
        stopSource.data = .featureCollection(FeatureCollection(features: stopFeatures.map(\.feature)))
        return stopSource
    }

    private static func generateStopFeatures(
        _ stopData: StopSourceData,
        _ stops: [String: MapStop],
        _ linesToSnap: [RouteLineData]
    ) -> [StopFeatureData] {
        var touchedStopIds: Set<String> = []

        let routeStops = Self.generateRouteAssociatedStops(stopData, stops, linesToSnap, &touchedStopIds)
        let otherStops = Self.generateRemainingStops(stopData, stops, &touchedStopIds)
        return otherStops + routeStops
    }

    private static func generateRouteAssociatedStops(
        _ stopData: StopSourceData,
        _ stops: [String: MapStop],
        _ linesToSnap: [RouteLineData],
        _ touchedStopIds: inout Set<String>
    ) -> [StopFeatureData] {
        linesToSnap.flatMap { lineData in
            lineData.stopIds.compactMap { childStopId -> StopFeatureData? in
                guard let stopOnRoute = stops[childStopId] else {
                    return nil
                }
                let mapStop = stops[stopOnRoute.stop.parentStationId ?? ""] ?? stopOnRoute
                let stop = mapStop.stop

                if touchedStopIds.contains(stop.id) || mapStop.routeTypes.isEmpty { return nil }

                let snappedCoord = lineData.line.toMapbox().closestCoordinate(to: stop.coordinate)?.coordinate
                touchedStopIds.insert(stop.id)
                return .init(
                    stop: mapStop,
                    feature: generateStopFeature(mapStop, stopData, at: snappedCoord)
                )
            }
        }
    }

    private static func generateRemainingStops(
        _ stopData: StopSourceData,
        _ stops: [String: MapStop],
        _ touchedStopIds: inout Set<String>
    ) -> [StopFeatureData] {
        stops.values.compactMap { mapStop in
            let stop = mapStop.stop
            if touchedStopIds.contains(stop.id)
                || mapStop.routeTypes.isEmpty
                || stop.parentStationId != nil { return nil }

            touchedStopIds.insert(stop.id)
            return .init(
                stop: mapStop,
                feature: generateStopFeature(mapStop, stopData)
            )
        }
    }

    private static func generateStopFeature(
        _ mapStop: MapStop,
        _ stopData: StopSourceData,
        at overrideLocation: CLLocationCoordinate2D? = nil
    ) -> Feature {
        let stop = mapStop.stop
        var stopFeature = Feature(geometry: Point(overrideLocation ?? stop.coordinate))
        stopFeature.identifier = FeatureIdentifier(stop.id)
        stopFeature.properties = generateStopFeatureProperties(mapStop, stopData)
        return stopFeature
    }

    private static func generateStopFeatureProperties(_ mapStop: MapStop, _ stopData: StopSourceData) -> JSONObject {
        var featureProps = JSONObject()
        let stop = mapStop.stop
        featureProps[Self.propIdKey] = JSONValue(String(describing: stop.id))
        featureProps[Self.propNameKey] = JSONValue(stop.name)
        featureProps[Self.propIsSelectedKey] = JSONValue.boolean(stop.id == stopData.selectedStopId)
        featureProps[Self.propIsTerminalKey] = JSONValue.boolean(mapStop.isTerminal)
        featureProps[Self.propMapRoutesKey] = JSONValue.array(JSONArray(
            mapStop.routeTypes.map { route in JSONValue(route.name) }
        ))
        var routeIds = JSONObject()
        for (routeType, routes) in mapStop.routes {
            routeIds[routeType.name] = JSONValue.array(JSONArray(
                routes.map { route in JSONValue(route.id) }
            ))
        }
        featureProps[Self.propRouteIdsKey] = JSONValue.object(routeIds)
        featureProps[Self.propServiceStatusKey] = serviceStatusValue(at: mapStop)

        // The symbolSortKey must be ascending, so higher priority icons need higher values. This takes the
        // ordinal of the top route and makes it negative. If there are no routes it's set to the total number
        // of route types so that the weird routeless stop is below everything else, and if it has multiple
        // route types, it's set to positive 1 to put the route container above everything else.
        let topRouteOrdinal = mapStop.routeTypes.isEmpty
            ? Double(MapStopRoute.AllCases().count)
            : Double(mapStop.routeTypes[0].ordinal)
        featureProps[Self.propSortOrderKey] = JSONValue.number(
            mapStop.routeTypes.count > 1 ? 1 : -topRouteOrdinal
        )

        return featureProps
    }

    private static func serviceStatusValue(at mapStop: MapStop) -> JSONValue {
        var alertStatus = JSONObject()
        for (routeType, status) in mapStop.alerts ?? [:] {
            alertStatus[routeType.name] = JSONValue(status.name)
        }
        return JSONValue.object(alertStatus)
    }
}
