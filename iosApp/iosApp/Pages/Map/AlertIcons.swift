//
//  AlertIcons.swift
//  iosApp
//
//  Created by Simon, Emma on 6/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
@_spi(Experimental) import MapboxMaps

enum AlertIcons {
    static let alertIconPrefix = "alert-"
    static let alertZoomClosePrefix = "large-"
    static let alertZoomWidePrefix = "small-"

    static let all: [String] =
        [alertZoomClosePrefix, alertZoomWidePrefix].flatMap { zoomPrefix in
            MapStopRoute.allCases.flatMap { routeType in
                StopAlertState.allCases
                    .filter { state in state != StopAlertState.normal }
                    .map { state in
                        "\(alertIconPrefix)\(zoomPrefix)\(routeType.name.lowercased())-\(state.name.lowercased())"
                    }
            }
        }

    // Expression that's true if the specified route index has no service status set
    private static func alertEmpty(_ index: Int) -> Exp {
        Exp(.not) { Exp(.has) {
            MapExp.routeAt(index)
            Exp(.get) { StopSourceGenerator.propServiceStatusKey }
        }}
    }

    // Expression that returns the alert status string for the given route index
    private static func alertStatus(_ index: Int) -> Exp {
        Exp(.get) {
            MapExp.routeAt(index)
            Exp(.get) { StopSourceGenerator.propServiceStatusKey }
        }
    }

    private static func getAlertIconName(_ zoomPrefix: String, _ index: Int, _ forBus: Bool) -> Exp {
        MapExp.busSwitchExp(forBus: forBus, Exp(.switchCase) {
            Exp(.any) {
                // Check if the index is greater than the number of routes at this stop
                Exp(.gte) { index; Exp(.length) { MapExp.routesExp } }
                // Or if the alert status at this index is empty
                alertEmpty(index)
                // Or if it's normal
                Exp(.eq) { alertStatus(index); StopAlertState.normal.name }
            }
            // If any of the above are true, don't display an alert icon
            ""
            // Otherwise, use the non-normal alert status and route to get its icon name
            Exp(.concat) {
                alertIconPrefix
                zoomPrefix
                Exp(.downcase) { MapExp.routeAt(index) }
                "-"
                Exp(.downcase) { alertStatus(index) }
            }
        })
    }

    static func getAlertLayerIcon(_ index: Int, forBus: Bool = false) -> Value<ResolvedImage> {
        .expression(Exp(.step) {
            Exp(.zoom)
            getAlertIconName(alertZoomWidePrefix, index, forBus)
            MapDefaults.closeZoomThreshold
            getAlertIconName(alertZoomClosePrefix, index, forBus)
        })
    }
}