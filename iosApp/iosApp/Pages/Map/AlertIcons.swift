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
                    // .filter { state in state != StopAlertState.normal }
                    .filter { state in state == StopAlertState.issue }
                    .map { state in
                        "\(alertIconPrefix)\(zoomPrefix)\(routeType.name.lowercased())-\(state.name.lowercased())"
                    }
            }
        }

    private static func getAlertIconName(_ zoomPrefix: String, _ index: Int) -> Exp {
        Exp(.switchCase) {
            Exp(.gte) { index; Exp(.length) { Exp(.get) { StopSourceGenerator.propMapRoutesKey } } }
            ""
            Exp(.concat) {
                alertIconPrefix
                zoomPrefix
                Exp(.downcase) { MapExp.routeAt(index) }
                "-"
                "issue"
            }
        }
    }

    static func getAlertLayerIcon(_ index: Int) -> Value<ResolvedImage> {
        .expression(Exp(.step) {
            Exp(.zoom)
            getAlertIconName(alertZoomWidePrefix, index)
            MapDefaults.closeZoomThreshold
            getAlertIconName(alertZoomClosePrefix, index)
        })
    }
}
