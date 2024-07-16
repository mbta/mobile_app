//
//  MapExp.swift
//  iosApp
//
//  Created by Simon, Emma on 6/3/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
@_spi(Experimental) import MapboxMaps

extension MapExp {
    // For the separate bus only stop and alert layers, this takes any arbitrary
    // string expression, and returns the expression or an empty string, depending
    // whether or not the stop is a bus stop and this is on the bus layer
    func busSwitchExp(forBus: Bool, _ resultExpression: MapboxMaps.Exp) -> MapboxMaps.Exp {
        Exp(.switchCase) {
            Exp(.all) {
                singleRouteTypeExp.toMapbox()
                Exp(.eq) { topRouteExp.toMapbox(); MapStopRoute.bus.name }
            }
            forBus ? resultExpression : Exp(.string) { "" }
            !forBus ? resultExpression : Exp(.string) { "" }
        }
    }

    // Get the label to display for this stop
    func stopLabelTextExp(forBus: Bool = false) -> MapboxMaps.Exp {
        Exp(.step) {
            Exp(.zoom)
            // Above mid zoom, never display any labels
            ""
            // At mid zoom, only display labels for terminal rail stops
            MapDefaults.shared.midZoomThreshold
            Exp(.switchCase) {
                Exp(.eq) { topRouteExp.toMapbox(); MapStopRoute.ferry.name }
                ""
                Exp(.get) { StopSourceGenerator.propIsTerminalKey }
                busSwitchExp(forBus: forBus, Exp(.get) { StopSourceGenerator.propNameKey })
                ""
            }
            // At close zoom, display labels for all non-bus stops
            MapDefaults.shared.closeZoomThreshold
            Exp(.switchCase) {
                Exp(.eq) { topRouteExp.toMapbox(); MapStopRoute.bus.name }
                ""
                busSwitchExp(forBus: forBus, Exp(.get) { StopSourceGenerator.propNameKey })
            }
        }
    }
}
