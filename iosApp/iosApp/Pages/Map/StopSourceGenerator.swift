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
    let stop: MapStop
    let feature: Feature
}

struct StopSourceData {
    let stops: [String: MapStop]
    let selectedStop: Stop?
    let routeLines: [RouteLineData]?
    let alertsByStop: [String: AlertAssociatedStop]
}

class StopSourceGenerator {
    var stopSourceData: StopSourceData
    var stopSource: GeoJSONSource

    private var stopFeatures: [StopFeatureData] = []

    static let stopSourceId = "stop-source"

    static let propIdKey = "id"
    static let propIsSelectedKey = "isSelected"
    // Map routes is an array of MapStopRoute enum names
    static let propMapRoutesKey = "mapRoutes"
    static let propNameKey = "name"
    // Route IDs are in a map keyed by MapStopRoute enum names, each with a list of IDs
    static let propRouteIdsKey = "routeIds"
    static let propServiceStatusKey = "serviceStatus"
    static let propSortOrderKey = "sortOrder"

    init(
        stops: [String: MapStop],
        selectedStop: Stop? = nil,
        routeLines: [RouteLineData]? = nil,
        alertsByStop: [String: AlertAssociatedStop] = [:]
    ) {
        stopSourceData = StopSourceData(
            stops: stops,
            selectedStop: selectedStop,
            routeLines: routeLines,
            alertsByStop: alertsByStop
        )

        stopFeatures = Self.generateStopFeatures(stopSourceData)
        stopSource = Self.generateStopSource(stopFeatures: stopFeatures)
    }

    static func generateRouteAssociatedStops(
        _ stopData: StopSourceData,
        _ touchedStopIds: inout Set<String>
    ) -> [StopFeatureData] {
        guard let routeLines = stopData.routeLines else { return [] }
        let stops = stopData.stops

        return routeLines.flatMap { lineData in
            lineData.stopIds.compactMap { childStopId -> StopFeatureData? in
                guard let stopOnRoute = stops[childStopId] else {
                    return nil
                }
                let mapStop = stops[stopOnRoute.stop.parentStationId ?? ""] ?? stopOnRoute
                let stop = mapStop.stop

                if touchedStopIds.contains(stop.id) || mapStop.routeTypes.isEmpty { return nil }

                let snappedCoord = lineData.line.closestCoordinate(to: stop.coordinate)?.coordinate
                touchedStopIds.insert(stop.id)
                return .init(
                    stop: mapStop,
                    feature: generateStopFeature(mapStop, stopData, at: snappedCoord)
                )
            }
        }
    }

    static func generateRemainingStops(
        _ stopData: StopSourceData,
        _ touchedStopIds: inout Set<String>
    ) -> [StopFeatureData] {
        stopData.stops.values.compactMap { mapStop in
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

    static func generateStopFeature(
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

    static func generateStopFeatures(_ stopData: StopSourceData) -> [StopFeatureData] {
        var touchedStopIds: Set<String> = []
        let routeStops = Self.generateRouteAssociatedStops(stopData, &touchedStopIds)
        let otherStops = Self.generateRemainingStops(stopData, &touchedStopIds)
        return otherStops + routeStops
    }

    static func generateStopFeatureProperties(_ mapStop: MapStop, _ stopData: StopSourceData) -> JSONObject {
        var featureProps = JSONObject()
        let stop = mapStop.stop
        featureProps[Self.propIdKey] = JSONValue(String(describing: stop.id))
        featureProps[Self.propNameKey] = JSONValue(stop.name)
        featureProps[Self.propIsSelectedKey] = JSONValue(stop.id == stopData.selectedStop?.id)
        featureProps[Self.propMapRoutesKey] = JSONValue.array(JSONArray(
            mapStop.routeTypes.map { route in JSONValue(route.name) }
        ))
        featureProps[Self.propServiceStatusKey] = JSONValue(
            serviceStatus(at: mapStop, from: stopData.alertsByStop).name
        )

        // The symbolSortKey must be ascending, so higher priority icons need higher values. This takes the total number
        // of types in the MapStopRoute enum, then subtracts the ordinal of the highest ranked route type for this stop.
        let allTypeCount = Double(MapStopRoute.AllCases().count)
        let topRouteOrdinal = mapStop.routeTypes.isEmpty
            ? allTypeCount + 1
            : Double(mapStop.routeTypes[0].ordinal)
        featureProps[Self.propSortOrderKey] = JSONValue.number(
            mapStop.routeTypes.count > 1
                ? allTypeCount + 1
                : allTypeCount - topRouteOrdinal
        )

        return featureProps
    }

    static func generateStopSource(stopFeatures: [StopFeatureData]) -> GeoJSONSource {
        var stopSource = GeoJSONSource(id: Self.stopSourceId)
        stopSource.data = .featureCollection(FeatureCollection(features: stopFeatures.map(\.feature)))
        return stopSource
    }

    static func serviceStatus(at mapStop: MapStop, from alerts: [String: AlertAssociatedStop]) -> StopServiceStatus {
        alerts[mapStop.stop.id]?.serviceStatus ?? StopServiceStatus.normal
    }
}
