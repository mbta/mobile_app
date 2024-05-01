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
    let routeLayers: [LineLayer]

    static let routeLayerId = "route-layer"
    static let alertingRouteLayerId = "route-layer-alerting"
    static let alertingBgRouteLayerId = "route-layer-alerting-bg"
    static func getRouteLayerId(_ routeId: String) -> String { "\(routeLayerId)-\(routeId)" }
    private static let lineWidth = 4.0

    init() {
        routeLayers = Self.createAllRouteLayers()
    }

    static func createAllRouteLayers() -> [LineLayer] {
        [createRouteLayer()] +
            // Draw all alerting layers on top so they are not covered by any overlapping route shape
            createAlertingRouteLayers()
    }

    static func createRouteLayer() -> LineLayer {
        baseRouteLayer(layerId: routeLayerId)
    }

    /**
     Styling applied only to the portions of the lines that are alerting
     */
    static func createAlertingRouteLayers() -> [LineLayer] {
        var alertingLayer = baseRouteLayer(layerId: alertingRouteLayerId)

        alertingLayer.filter = Exp(.get) { RouteSourceGenerator.propIsAlertingKey }
        alertingLayer.lineDasharray = .constant([2.0, 3.0])
        alertingLayer.lineColor = .constant(StyleColor(UIColor.white))
        alertingLayer.lineOpacity = .constant(0.7)

        var alertBackgroundLayer = baseRouteLayer(layerId: alertingBgRouteLayerId)

        alertBackgroundLayer.lineColor = .expression(Exp(.get) {
            RouteSourceGenerator.propRouteColor
        })

        return [alertBackgroundLayer, alertingLayer]
    }

    private static func baseRouteLayer(layerId: String) -> LineLayer {
        var layer = LineLayer(
            id: layerId,
            source: RouteSourceGenerator.routeSourceId
        )
        layer.lineWidth = .constant(lineWidth)
        layer.lineColor = .expression(Exp(.get) {
            RouteSourceGenerator.propRouteColor
        })
        layer.lineBorderWidth = .constant(1.0)
        layer.lineBorderColor = .constant(StyleColor(.white))
        layer.lineJoin = .constant(.round)
        layer.lineCap = .constant(.round)
        layer.lineOffset = .expression(lineOffsetExpression())
        layer.lineSortKey = .expression(Exp(.get) {
            RouteSourceGenerator.propRouteSortKey
        })

        return layer
    }

    /**
     Hardcoding offsets based on route properties to minimize the occurences of overlapping rail lines
     when drawn on the map
     */
    private static func lineOffsetExpression() -> Exp {
        Expression(.switchCase) {
            Expression(.inExpression) {
                Exp(.get) {
                    "routeId"
                }
                Exp(.literal) {
                    ["CR-Lowell", "CR-Fitchburg"]
                }
            }
            // These overlap with GL. GL is offset below, so do nothing
            0
            Expression(.inExpression) {
                Exp(.get) {
                    "routeId"
                }
                Exp(.literal) {
                    ["CR-Greenbush", "CR-Kingston", "CR-Middleborough"]
                }
            }
            // These overlap with RL, shift West
            RouteLayerGenerator.lineWidth * 1.5
            Expression(.eq) {
                Exp(.get) {
                    "routeType"
                }
                Exp(.literal) {
                    "COMMUTER_RAIL"
                }
            }
            // Some overlap with OL and should shift East.
            // Shift the rest east too so they scale porportionally
            -RouteLayerGenerator.lineWidth
            Expression(.inExpression) {
                Exp(.literal) {
                    "Green"
                }
                Exp(.get) {
                    "routeId"
                }
            }
            // Account for overlapping North Station - Haymarket
            // Offset to the East
            RouteLayerGenerator.lineWidth
            // Default to no offset
            0
        }
    }
}
