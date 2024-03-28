//
//  RouteLayerGenerator.swift
//  iosApp
//
//  Created by Simon, Emma on 3/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI
@_spi(Experimental) import MapboxMaps

class RouteLayerGenerator {
    let routeData: RouteResponse
    let routeLayers: [LineLayer]

    static let routeLayerId = "route-layer"
    static func getRouteLayerId(_ routeId: String) -> String { "\(routeLayerId)-\(routeId)" }

    init(routeData: RouteResponse) {
        self.routeData = routeData
        routeLayers = Self.createRouteLayers(routes: routeData.routes)
    }

    static func createRouteLayers(routes: [Route]) -> [LineLayer] {
        // Sort by reverse sort order so that lowest ordered routes are drawn first/lowest
        routes
            .sorted { $0.sortOrder >= $1.sortOrder }
            .map { createRouteLayer(route: $0) }
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
