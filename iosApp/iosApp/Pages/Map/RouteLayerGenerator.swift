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
        routeLayers = Self.createRouteLayers(routesWithShapes: mapFriendlyRoutesResponse.routesWithSegmentedShapes,
                                             routesById: routesById)
    }

    static func createRouteLayers(routesWithShapes: [MapFriendlyRouteResponse.RouteWithSegmentedShapes],
                                  routesById: [String: Route]) -> [LineLayer] {
        routesWithShapes
            .filter { routesById[$0.routeId] != nil }
            .sorted {
                // Sort by reverse sort order so that lowest ordered routes are drawn first/lowest
                routesById[$0.routeId]!.sortOrder >= routesById[$1.routeId]!.sortOrder
            }
            .map { createRouteLayer(route: routesById[$0.routeId]!) }
    }

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
