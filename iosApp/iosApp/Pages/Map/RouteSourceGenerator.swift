//
//  RouteSourceGenerator.swift
//  iosApp
//
//  Created by Simon, Emma on 3/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Polyline
import shared
@_spi(Experimental) import MapboxMaps

class RouteLineData {
    let id: String
    let sourceRoutePatternId: String
    let line: LineString
    let stopIds: [String]
    let isAlerting: Bool

    init(id: String, sourceRoutePatternId: String, line: LineString, stopIds: [String], isAlerting: Bool) {
        self.id = id
        self.sourceRoutePatternId = sourceRoutePatternId
        self.line = line
        self.stopIds = stopIds
        self.isAlerting = isAlerting
    }
}

class RouteSourceData {
    let routeId: String
    let lines: [RouteLineData]
    let source: GeoJSONSource

    init(routeId: String, lines: [RouteLineData], source: GeoJSONSource) {
        self.routeId = routeId
        self.lines = lines
        self.source = source
    }
}

class RouteSourceGenerator {
    let routeData: MapFriendlyRouteResponse
    let routeSourceDetails: [RouteSourceData]
    let routeSources: [GeoJSONSource]

    static let routeSourceId = "route-source"
    static func getRouteSourceId(_ routeId: String) -> String { "\(routeSourceId)-\(routeId)" }

    static let propIsAlertingKey = "isAlerting"

    init(routeData: MapFriendlyRouteResponse, stopsById: [String: Stop], alertsByStop: [String: AlertAssociatedStop]) {
        self.routeData = routeData
        routeSourceDetails = Self.generateRouteSources(routeData: routeData, stopsById: stopsById,
                                                       alertsByStop: alertsByStop)
        routeSources = routeSourceDetails.map(\.source)
    }

    static func generateRouteSources(routeData: MapFriendlyRouteResponse,
                                     stopsById: [String: Stop],
                                     alertsByStop: [String: AlertAssociatedStop]) -> [RouteSourceData] {
        routeData.routesWithSegmentedShapes
            .map { generateRouteSource(routeId: $0.routeId,
                                       routeShapes: $0.segmentedShapes,
                                       stopsById: stopsById,
                                       alertsByStop: alertsByStop) }
    }

    static func generateRouteSource(routeId: String, routeShapes: [SegmentedRouteShape],
                                    stopsById: [String: Stop],
                                    alertsByStop: [String: AlertAssociatedStop]) -> RouteSourceData {
        let routeLines = generateRouteLines(routeId: routeId, routeShapes: routeShapes, stopsById: stopsById,
                                            alertsByStop: alertsByStop)
        let routeFeatures: [Feature] = routeLines.map { lineData in
            var feature = Feature(geometry: lineData.line)
            var featureProps = JSONObject()
            featureProps[Self.propIsAlertingKey] = JSONValue(Bool(lineData.isAlerting))
            feature.properties = featureProps
            return feature
        }
        var routeSource = GeoJSONSource(id: Self.getRouteSourceId(routeId))
        routeSource.data = .featureCollection(FeatureCollection(features: routeFeatures))
        return .init(routeId: routeId, lines: routeLines, source: routeSource)
    }

    static func generateRouteLines(routeId _: String, routeShapes: [SegmentedRouteShape],
                                   stopsById: [String: Stop],
                                   alertsByStop: [String: AlertAssociatedStop]) -> [RouteLineData] {
        routeShapes
            .flatMap { routePatternShape in
                routeShapeToLineData(routePatternShape: routePatternShape, stopsById: stopsById,
                                     alertsByStop: alertsByStop)
            }
    }

    private static func routeShapeToLineData(routePatternShape: SegmentedRouteShape,
                                             stopsById: [String: Stop],
                                             alertsByStop: [String: AlertAssociatedStop]) -> [RouteLineData] {
        guard let polyline = routePatternShape.shape.polyline,
              let coordinates = Polyline(encodedPolyline: polyline).coordinates
        else {
            return []
        }

        let fullLineString = LineString(coordinates)
        let alertAwareSegments = routePatternShape.routeSegments.flatMap { segment in
            segment.splitAlertingSegments(alertsByStop: alertsByStop)
        }
        return alertAwareSegments.compactMap { segment in
            routeSegmentToRouteLineData(segment: segment, fullLineString: fullLineString,
                                        stopsById: stopsById)
        }
    }

    private static func routeSegmentToRouteLineData(segment: AlertAwareRouteSegment, fullLineString: LineString,
                                                    stopsById: [String: Stop]) -> RouteLineData? {
        guard let firstStopId = segment.stopIds.first,
              let firstStop = stopsById[firstStopId],
              let lastStopId = segment.stopIds.last,
              let lastStop = stopsById[lastStopId],
              let lineSegment = fullLineString.sliced(from: firstStop.coordinate,
                                                      to: lastStop.coordinate)
        else {
            return nil
        }
        return RouteLineData(id: segment.id,
                             sourceRoutePatternId: segment.sourceRoutePatternId,
                             line: lineSegment,
                             stopIds: segment.stopIds, isAlerting: segment.isAlerting)
    }
}
