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
    static let shuttledRouteLayerId = "route-layer-shuttled"
    static let suspendedRouteLayerId = "route-layer-suspended"
    static let alertingBgRouteLayerId = "route-layer-alerting-bg"
    static func getRouteLayerId(_ routeId: String) -> String { "\(routeLayerId)-\(routeId)" }
    private static let closeZoomCutoff = MapDefaults.closeZoomThreshold

    init() {
        routeLayers = Self.createAllRouteLayers()
    }

    static func createAllRouteLayers() -> [LineLayer] {
        [createRouteLayer()] +
            // Draw all alerting layers on top so they are not covered by any overlapping route shape
            createAlertingRouteLayers()
    }

    static func createRouteLayer() -> LineLayer {
        var layer = baseRouteLayer(layerId: routeLayerId)
        layer.lineWidth = .expression(Exp(.step) {
            Exp(.zoom)
            3
            closeZoomCutoff
            4
        })
        return layer
    }

    /**
     Styling applied only to the portions of the lines that are alerting

     Creates separate layers for shuttle and suspension segments because `LineLayer.lineDasharray`
     [doesn't support data-driven styling](https://docs.mapbox.com/style-spec/reference/layers/#paint-line-line-dasharray)
     */
    static func createAlertingRouteLayers() -> [LineLayer] {
        var shuttledLayer = baseRouteLayer(layerId: shuttledRouteLayerId)

        shuttledLayer.filter = Exp(.eq) {
            Exp(.get) { RouteFeaturesBuilder.shared.propAlertStateKey }
            SegmentAlertState.shuttle.name
        }
        shuttledLayer.lineWidth = .expression(Exp(.step) {
            Exp(.zoom)
            4
            closeZoomCutoff
            6
        })
        shuttledLayer.lineDasharray = .constant([2.0, 1.33])

        var suspendedLayer = baseRouteLayer(layerId: suspendedRouteLayerId)

        suspendedLayer.filter = Exp(.eq) {
            Exp(.get) { RouteFeaturesBuilder.shared.propAlertStateKey }
            SegmentAlertState.suspension.name
        }
        suspendedLayer.lineWidth = .expression(Exp(.step) {
            Exp(.zoom)
            4
            closeZoomCutoff
            6
        })
        suspendedLayer.lineDasharray = .constant([1.33, 2.0])
        suspendedLayer.lineColor = .constant(StyleColor(.deemphasized))

        var alertBackgroundLayer = baseRouteLayer(layerId: alertingBgRouteLayerId)

        alertBackgroundLayer.filter = Exp(.inExpression) {
            Exp(.get) { RouteFeaturesBuilder.shared.propAlertStateKey }
            [SegmentAlertState.suspension.name, SegmentAlertState.shuttle.name]
        }
        alertBackgroundLayer.lineWidth = .expression(Exp(.step) {
            Exp(.zoom)
            8
            closeZoomCutoff
            10
        })
        alertBackgroundLayer.lineColor = .constant(StyleColor(.fill3))

        return [alertBackgroundLayer, shuttledLayer, suspendedLayer]
    }

    private static func baseRouteLayer(layerId: String) -> LineLayer {
        var layer = LineLayer(
            id: layerId,
            source: RouteFeaturesBuilder.shared.routeSourceId
        )
        layer.lineColor = .expression(Exp(.get) {
            RouteFeaturesBuilder.shared.propRouteColor
        })
        layer.lineJoin = .constant(.round)
        layer.lineOffset = .expression(lineOffsetExpression())
        layer.lineSortKey = .expression(Exp(.get) {
            RouteFeaturesBuilder.shared.propRouteSortKey
        })

        return layer
    }

    /**
     Hardcoding offsets based on route properties to minimize the occurences of overlapping rail lines
     when drawn on the map
     */
    private static func lineOffsetExpression() -> Exp {
        let maxLineWidth = 6.0

        return Expression(.switchCase) {
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
            maxLineWidth * 1.5
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
            -maxLineWidth
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
            maxLineWidth
            // Default to no offset
            0
        }
    }
}
