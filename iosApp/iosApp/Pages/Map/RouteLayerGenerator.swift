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
    static func getRouteLayerId(_ routePatternId: String, _ segmentId: String) -> String { "\(routeLayerId)-\(routePatternId)-\(segmentId)" }

    init(routeData: RouteResponse) {
        self.routeData = routeData
        routeLayers = Self.createRouteLayers(routeShapes: routeData.routeShapes)
    }

    static func createRouteLayers(routeShapes: [MapFriendlyRouteShape]) -> [LineLayer] {
        routeShapes
            .flatMap { createRouteLayers(routeShape: $0) }
    }

    static func createRouteLayers(routeShape: MapFriendlyRouteShape) -> [LineLayer] {
        let segments = routeShape.routeSegments

        return segments.map { segment in

            var segmentLayer = LineLayer(
                id: Self.getRouteLayerId(routeShape.routePatternId, segment.id),
                source: RouteSourceGenerator.getRouteSourceId(routeShape.routePatternId, segment.id)
            )
            segmentLayer.lineWidth = .constant(4.0)
            segmentLayer.lineColor = .constant(StyleColor(UIColor(hex: routeShape.color)))
            segmentLayer.lineBorderWidth = .constant(1.0)
            segmentLayer.lineBorderColor = .constant(StyleColor(.white))
            segmentLayer.lineJoin = .constant(.round)
            segmentLayer.lineCap = .constant(.round)
            return segmentLayer
        }
    }
}
