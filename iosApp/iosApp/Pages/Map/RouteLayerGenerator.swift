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
    static let lineWidth = 4.0

    init(mapFriendlyRoutesResponse: MapFriendlyRouteResponse, routesById: [String: Route]) {
        self.mapFriendlyRoutesResponse = mapFriendlyRoutesResponse
        self.routesById = routesById
        routeLayers = Self.createAllRouteLayers(routesWithShapes: mapFriendlyRoutesResponse.routesWithSegmentedShapes,
                                                routesById: routesById)
    }

    static func createAllRouteLayers(routesWithShapes: [MapFriendlyRouteResponse.RouteWithSegmentedShapes],
                                     routesById: [String: Route]) -> [LineLayer] {
        routesWithShapes
            .filter { routesById[$0.routeId] != nil }
            .sorted {
                // Sort by reverse sort order so that lowest ordered routes are drawn first/lowest
                routesById[$0.routeId]!.sortOrder >= routesById[$1.routeId]!.sortOrder
            }
            .flatMap { createRouteLayers(route: routesById[$0.routeId]!) }
    }

    /**
     Define the line layers for styling the route's line shapes.
     Returns a list of 2 LineLayers - one with a styling to be applied to the entirety of all shapes in the route,
     and a second that is applied only to the portions of the lines that are alerting.
     */
    static func createRouteLayers(route: Route) -> [LineLayer] {
        let lineOffset = lineOffset(route)

        var alertingLayer = LineLayer(
            id: Self.getRouteLayerId("\(route.id)-alerting"),
            source: RouteSourceGenerator.getRouteSourceId(route.id)
        )

        alertingLayer.filter = Exp(.get) { RouteSourceGenerator.propIsAlertingKey }
        alertingLayer.lineDasharray = .constant([2.0, 3.0])
        alertingLayer.lineWidth = .constant(lineWidth)
        alertingLayer.lineColor = .constant(StyleColor(UIColor.white))
        alertingLayer.lineBorderWidth = .constant(1.0)
        alertingLayer.lineBorderColor = .constant(StyleColor(.white))
        alertingLayer.lineJoin = .constant(.round)
        alertingLayer.lineCap = .constant(.round)
        alertingLayer.lineOffset = .constant(lineOffset)
        alertingLayer.lineOpacity = .constant(0.7)

        var nonAlertingLayer = LineLayer(
            id: Self.getRouteLayerId("\(route.id)"),
            source: RouteSourceGenerator.getRouteSourceId(route.id)
        )

        nonAlertingLayer.lineWidth = .constant(lineWidth)
        nonAlertingLayer.lineColor = .constant(StyleColor(UIColor(hex: route.color)))
        nonAlertingLayer.lineBorderWidth = .constant(1.0)
        nonAlertingLayer.lineBorderColor = .constant(StyleColor(.white))
        nonAlertingLayer.lineJoin = .constant(.round)
        nonAlertingLayer.lineCap = .constant(.round)
        nonAlertingLayer.lineOffset = .constant(lineOffset)

        return [nonAlertingLayer, alertingLayer]
    }

    /**
     Hardcoding offsets based on route properties to minimize the occurences of overlapping rail lines when drawn on the map
     */
    private static func lineOffset(_ route: Route) -> Double {
        var greenOverlappingCR: Set = ["CR-Lowell", "CR-Fitchburg"]
        var redOverlappingCR: Set = ["CR-Greenbush", "CR-Kingston", "CR-Middleborough"]

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
