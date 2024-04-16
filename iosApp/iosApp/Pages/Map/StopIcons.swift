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
    static let stopZoomThreshold: Double = 7.0
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
    ]

    static let stationIconExpression: Value<ResolvedImage> = .expression(
        Exp(.match) {
            Exp(.get) { StopSourceGenerator.propServiceStatusKey }
            String(describing: StopServiceStatus.noService)
            stationIconNoServiceId
            String(describing: StopServiceStatus.partialService)
            stationIconIssuesId
            stationIconId
        }
    )

    static let stopIconExpression: Value<ResolvedImage> = .expression(Exp(.step) {
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

    static let stopIconFixedSize: Value<Double> = .constant(1)

    static let stopIconLinearSizeExpression: Value<Double> = .expression(Exp(.interpolate) {
        Exp(.linear)
        Exp(.zoom)
        [
            11: 0.1,
            15.0: 1,
            16: 10,
        ]
    })

    static let stopIconExponentialSizeExpression: Value<Double> = .expression(Exp(.interpolate) {
        Exp(.exponential) { 10 }
        Exp(.zoom)
        [
            11.0: 0.1,
            15.0: 1,
            16: 10,
        ]
    })

    static let stopIconCubicSizeExpression: Value<Double> = .expression(Exp(.interpolate) {
        Exp(.cubicBezier) { 0.42; 0; 0.58; 1 }
        Exp(.zoom)
        [
            11.0: 0.1,
            15.0: 1,
            16: 10,
        ]
    })

    static func getStopLayerIcon(_ locationType: LocationType) -> Value<ResolvedImage> {
        switch locationType {
        case .station:
            stationIconExpression
        case .stop:
            stopIconExpression
        default:
            .constant(.name(""))
        }
    }
}
