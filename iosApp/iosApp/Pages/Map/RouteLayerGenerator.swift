
//
//  RouteLayerGenerator.swift
//  iosApp
//
//  Created by Simon, Emma on 3/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import OSLog
import shared
import SwiftUI
@_spi(Experimental) import MapboxMaps

class RouteLayerGenerator {
    let mapFriendlyRoutesResponse: MapFriendlyRouteResponse
    let routesById: [String: Route]
    let routeLayers: [LineLayer]

    static let routeLayerId = "route-layer"
    static func getRouteLayerId(_ routeId: String) -> String { "\(routeLayerId)-\(routeId)" }

    init(mapFriendlyRoutesResponse: MapFriendlyRouteResponse, routesById: [String: Route]) {
        self.mapFriendlyRoutesResponse = mapFriendlyRoutesResponse
        self.routesById = routesById
        routeLayers = Self.createRouteLayers(routeShapes: mapFriendlyRoutesResponse.routeShapes, routesById: routesById)
    }

    static func createRouteLayers(routeShapes: [MapFriendlyRouteShape], routesById: [String: Route]) -> [LineLayer] {
        let routeShapesByRoute: [String: [MapFriendlyRouteShape]] = Dictionary(grouping: routeShapes) { $0.sourceRouteId }

        return routeShapesByRoute
            .filter { routesById[$0.key] != nil }
            .sorted {
                // Sort by reverse sort order so that lowest ordered routes are drawn first/lowest
                routesById[$0.key]!.sortOrder >= routesById[$1.key]!.sortOrder
            }
            .map { createRouteLayer(route: routesById[$0.key]!) }
    }

    // TODO: Alert styling
    static func createRouteLayer(route: Route) -> LineLayer {
        var routeLayer = LineLayer(
            id: Self.getRouteLayerId(route.id),
            source: RouteSourceGenerator.getRouteSourceId(route.id)
        )
        routeLayer.lineWidth = .constant(4.0)
        routeLayer.lineColor = .constant(StyleColor(UIColor(hex: route.color)))
        routeLayer.lineBorderWidth = .constant(1.0)
        routeLayer.lineBorderColor = .constant(StyleColor(.white))
        routeLayer.lineJoin = .constant(.round)
        routeLayer.lineCap = .constant(.round)
        return routeLayer
    }
}
