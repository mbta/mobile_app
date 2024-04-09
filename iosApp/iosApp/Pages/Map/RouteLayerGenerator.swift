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
            .flatMap { createRouteLayer(route: routesById[$0.routeId]!) }
    }

    static func createRouteLayer(route: Route) -> [LineLayer] {
        var alertingLayer = LineLayer(
            id: Self.getRouteLayerId("\(route.id)-alerting"),
            source: RouteSourceGenerator.getRouteSourceId(route.id)
        )
        alertingLayer.filter = Exp(.get) { RouteSourceGenerator.propIsAlertingKey }
        alertingLayer.lineDasharray = .constant([2.0, 3.0])

        alertingLayer.lineWidth = .constant(4.0)
        alertingLayer.lineColor = .constant(StyleColor(UIColor.white))
        alertingLayer.lineBorderWidth = .constant(1.0)
        alertingLayer.lineBorderColor = .constant(StyleColor(.white))
        alertingLayer.lineJoin = .constant(.round)
        alertingLayer.lineCap = .constant(.round)

        var nonAlertingLayer = LineLayer(
            id: Self.getRouteLayerId("\(route.id)"),
            source: RouteSourceGenerator.getRouteSourceId(route.id)
        )

        nonAlertingLayer.lineWidth = .constant(4.0)
        nonAlertingLayer.lineColor = .constant(StyleColor(UIColor(hex: route.color)))
        nonAlertingLayer.lineBorderWidth = .constant(1.0)
        nonAlertingLayer.lineBorderColor = .constant(StyleColor(.white))
        nonAlertingLayer.lineJoin = .constant(.round)
        nonAlertingLayer.lineCap = .constant(.round)
        return [nonAlertingLayer, alertingLayer]
    }
}
