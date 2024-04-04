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
    let routePatternId: String
    let line: LineString
    let stopIds: [String]

    init(routePatternId: String, line: LineString, stopIds: [String]) {
        self.routePatternId = routePatternId
        self.line = line
        self.stopIds = stopIds
    }
}

class RouteSourceData {
    let route: Route
    let lines: [RouteLineData]
    let source: GeoJSONSource

    init(route: Route, lines: [RouteLineData], source: GeoJSONSource) {
        self.route = route
        self.lines = lines
        self.source = source
    }
}

class RouteSourceGenerator {
    let routeData: RouteResponse

    let routeSourceDetails: [RouteSourceData]
    let routeSources: [GeoJSONSource]

    static let routeSourceId = "route-source"
    static func getRouteSourceId(_ routeId: String) -> String { "\(routeSourceId)-\(routeId)" }

    init(routeData: RouteResponse) {
        self.routeData = routeData
        routeSourceDetails = Self.generateRouteSources(routeData: routeData)
        routeSources = routeSourceDetails.map(\.source)
    }

    static func generateRouteSources(routeData: RouteResponse) -> [RouteSourceData] {
        routeData.routes.map { Self.generateRouteSource(route: $0, routeData: routeData) }
    }

    static func generateRouteSource(route: Route, routeData: RouteResponse) -> RouteSourceData {
        let routeLines = Self.generateRouteLines(route: route, routeData: routeData)
        let routeFeatures: [Feature] = routeLines.map { Feature(geometry: $0.line) }
        var routeSource = GeoJSONSource(id: Self.getRouteSourceId(route.id))
        routeSource.data = .featureCollection(FeatureCollection(features: routeFeatures))

        return .init(route: route, lines: routeLines, source: routeSource)
    }

    static func generateRouteLines(route: Route, routeData: RouteResponse) -> [RouteLineData] {
        (route.routePatternIds ?? [])
            .map { routeData.routePatterns[$0] }
            .filter { $0?.typicality == .typical }
            .compactMap { pattern in
                guard let pattern,
                      let representativeTrip = routeData.trips[pattern.representativeTripId],
                      let shapeId = representativeTrip.shapeId,
                      let shape = routeData.shapes[shapeId],
                      let polyline = shape.polyline,
                      let coordinates = Polyline(encodedPolyline: polyline).coordinates
                else { return nil }

                return .init(
                    routePatternId: pattern.id,
                    line: LineString(coordinates),
                    stopIds: representativeTrip.stopIds ?? []
                )
            }
    }
}
