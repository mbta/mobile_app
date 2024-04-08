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

    init(id: String, sourceRoutePatternId: String, line: LineString, stopIds: [String]) {
        self.id = id
        self.sourceRoutePatternId = sourceRoutePatternId
        self.line = line
        self.stopIds = stopIds
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

    init(routeData: MapFriendlyRouteResponse, stopsById: [String: Stop]) {
        self.routeData = routeData
        routeSourceDetails = Self.generateRouteSources(routeData: routeData, stopsById: stopsById)
        routeSources = routeSourceDetails.map(\.source)
    }

    static func generateRouteSources(routeData: MapFriendlyRouteResponse,
                                     stopsById: [String: Stop]) -> [RouteSourceData]
    {
        routeData.routesWithSegmentedShapes
            .map { Self.generateRouteSource(routeId: $0.routeId,
                                            routeShapes: $0.segmentedShapes,
                                            stopsById: stopsById) }
    }

    static func generateRouteSource(routeId: String, routeShapes: [SegmentedRouteShape], stopsById: [String: Stop]) -> RouteSourceData {
        let routeLines = Self.generateRouteLines(routeId: routeId, routeShapes: routeShapes, stopsById: stopsById)
        let routeFeatures: [Feature] = routeLines.map { Feature(geometry: $0.line) }
        var routeSource = GeoJSONSource(id: Self.getRouteSourceId(routeId))
        routeSource.data = .featureCollection(FeatureCollection(features: routeFeatures))
        return .init(routeId: routeId, lines: routeLines, source: routeSource)
    }

    static func generateRouteLines(routeId _: String, routeShapes: [SegmentedRouteShape], stopsById: [String: Stop]) -> [RouteLineData] {
        routeShapes
            .flatMap { routePatternShape in
                self.routeShapeToLineData(routePatternShape: routePatternShape, stopsById: stopsById)
            }
    }

    private static func routeShapeToLineData(routePatternShape: SegmentedRouteShape, stopsById: [String: Stop]) -> [RouteLineData] {
        guard let polyline = routePatternShape.shape.polyline,
              let coordinates = Polyline(encodedPolyline: polyline).coordinates
        else {
            return []
        }

        let fullLineString = LineString(coordinates)
        return routePatternShape.routeSegments.compactMap { routeSegment in
            routeSegmentToRouteLineData(routeSegment: routeSegment, fullLineString: fullLineString, stopsById: stopsById)
        }
    }

    private static func routeSegmentToRouteLineData(routeSegment: RouteSegment, fullLineString: LineString, stopsById: [String: Stop]) -> RouteLineData? {
        guard let firstStopId = routeSegment.stopIds.first,
              let firstStop = stopsById[firstStopId],
              let lastStopId = routeSegment.stopIds.last,
              let lastStop = stopsById[lastStopId],
              let lineSegment = fullLineString.sliced(from: firstStop.coordinate,
                                                      to: lastStop.coordinate)
        else {
            return nil
        }
        return RouteLineData(id: routeSegment.id,
                             sourceRoutePatternId: routeSegment.sourceRoutePatternId,
                             line: lineSegment,
                             stopIds: routeSegment.stopIds)
    }
}
