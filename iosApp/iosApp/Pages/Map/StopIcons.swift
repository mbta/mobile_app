//
//  StopIcons.swift
//  iosApp
//
//  Created by Simon, Emma on 4/3/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
@_spi(Experimental) import MapboxMaps

enum StopIcons {
    static let stopZoomThreshold: Double = 13.0
    static let tombstoneZoomThreshold: Double = 16.0

    static let stationIconId = "t-station"
    static let stationIconIssuesId = "t-station-issues"
    static let stationIconNoServiceId = "t-station-no-service"
    static let stopIconId = "bus-stop"
    static let stopIconIssuesId = "bus-stop-issues"
    static let stopIconNoServiceId = "bus-stop-no-service"
    static let stopIconSmallId = "bus-stop-small"

    static let all: [String] = [
        stationIconId, stationIconIssuesId, stationIconNoServiceId,
        stopIconId, stopIconIssuesId, stopIconNoServiceId, stopIconSmallId,
        "map-stop-container-2", "map-stop-container-3",
        "map-stop-pill-Red", "map-stop-pill-Orange", "map-stop-pill-Green",
        "map-stop-pill-Blue",
    ]

    static func getStopLayerIcon(_ locationType: LocationType) -> Value<ResolvedImage> {
        switch locationType {
        case .station:
//            .expression(
//                Exp(.match) {
//                    Exp(.get) { StopSourceGenerator.propServiceStatusKey }
//                    String(describing: StopServiceStatus.noService)
//                    stationIconNoServiceId
//                    String(describing: StopServiceStatus.partialService)
//                    stationIconIssuesId
//                    stationIconId
//                }
//            )
            .expression(
                Exp(.step) {
                    Exp(.get) { "routeCount" }
                    stationIconId
                    2
                    "map-stop-container-2"
                    3
                    "map-stop-container-3"
                }
            )
        case .stop:
            .expression(Exp(.step) {
                Exp(.zoom)
                stopIconSmallId
                tombstoneZoomThreshold
                Exp(.match) {
                    Exp(.get) { StopSourceGenerator.propServiceStatusKey }
                    String(describing: StopServiceStatus.noService)
                    stopIconNoServiceId
                    String(describing: StopServiceStatus.partialService)
                    stopIconIssuesId
                    stopIconId
                }
            })
        default:
            .constant(.name(""))
        }
    }
}
