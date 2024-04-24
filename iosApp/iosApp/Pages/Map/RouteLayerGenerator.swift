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
    private static let lineWidth = 4.0

    init(mapFriendlyRoutesResponse: MapFriendlyRouteResponse, routesById: [String: Route]) {
        self.mapFriendlyRoutesResponse = mapFriendlyRoutesResponse
        self.routesById = routesById
        routeLayers = Self.createAllRouteLayers(routesWithShapes: mapFriendlyRoutesResponse.routesWithSegmentedShapes,
                                                routesById: routesById)
    }

    static func createAllRouteLayers(routesWithShapes: [MapFriendlyRouteResponse.RouteWithSegmentedShapes],
                                     routesById: [String: Route]) -> [LineLayer] {
        let sortedRoutes = routesWithShapes
            .filter { routesById[$0.routeId] != nil }
            .sorted {
                // Sort by reverse sort order so that lowest ordered routes are drawn first/lowest
                routesById[$0.routeId]!.sortOrder >= routesById[$1.routeId]!.sortOrder
            }

        return sortedRoutes
            .map { createRouteLayer(route: routesById[$0.routeId]!) } +
            // Draw all alerting layers on top so they are not covered by any overlapping route shape
            sortedRoutes
            .flatMap { createAlertingRouteLayers(route: routesById[$0.routeId]!) }
    }

    static func createRouteLayer(route: Route) -> LineLayer {
        baseRouteLayer(layerId: getRouteLayerId(route.id), route: route)
    }

    /**
     Styling applied only to the portions of the lines that are alerting
     */
    static func createAlertingRouteLayers(route: Route) -> [LineLayer] {
        var alertingLayer = baseRouteLayer(layerId: Self.getRouteLayerId("\(route.id)-alerting"), route: route)

        alertingLayer.filter = Exp(.get) { RouteSourceGenerator.propIsAlertingKey }
        alertingLayer.lineDasharray = .constant([2.0, 3.0])
        alertingLayer.lineColor = .constant(StyleColor(UIColor.white))
        alertingLayer.lineOpacity = .constant(0.7)

        var alertBackgroundLayer = baseRouteLayer(layerId: Self.getRouteLayerId("\(route.id)-alerting-bg"),
                                                  route: route)

        alertBackgroundLayer.lineColor = .constant(StyleColor(UIColor(hex: route.color)))

        return [alertBackgroundLayer, alertingLayer]
    }

    private static func baseRouteLayer(layerId: String, route: Route) -> LineLayer {
        var layer = LineLayer(
            id: layerId,
            source: RouteSourceGenerator.getRouteSourceId(route.id)
        )
        layer.lineWidth = .constant(lineWidth)
        layer.lineColor = .constant(StyleColor(UIColor(hex: route.color)))
        layer.lineBorderWidth = .constant(1.0)
        layer.lineBorderColor = .constant(StyleColor(.white))
        layer.lineJoin = .constant(.round)
        layer.lineCap = .constant(.round)
        layer.lineOffset = .constant(lineOffset(route))

        return layer
    }

    /**
     Hardcoding offsets based on route properties to minimize the occurences of overlapping rail lines when drawn on the map
     */
    private static func lineOffset(_ route: Route) -> Double {
        let greenOverlappingCR: Set = ["CR-Lowell", "CR-Fitchburg"]
        let redOverlappingCR: Set = ["CR-Greenbush", "CR-Kingston", "CR-Middleborough"]

        return if route.type == RouteType.commuterRail {
            if greenOverlappingCR.contains(route.id) {
                // These overlap with GL. GL is offset below, so do nothing
                0
            } else if redOverlappingCR.contains(route.id) {
                // These overlap with RL. RL is offset below, shift West
                RouteLayerGenerator.lineWidth * 1.5

            } else {
                // Some overlap with OL and should shift East.
                // Shift the rest east too so they scale porportionally
                -RouteLayerGenerator.lineWidth
            }
        } else if route.id.contains("Green") {
            // Account for overlapping North Station - Haymarket
            // Offset to the East
            RouteLayerGenerator.lineWidth
        } else {
            0
        }
    }
}
