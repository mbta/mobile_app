//
//  MapAnalytics.swift
//  iosApp
//
//  Created by Horn, Melody on 2025-01-14.
//  Copyright © 2025 MBTA. All rights reserved.
//

protocol MapAnalytics {
    func tappedOnStop(stopId: String)
    func tappedVehicle(routeId: String)
}

extension AnalyticsProvider: MapAnalytics {
    func tappedOnStop(stopId: String) {
        logEvent(
            "tapped_on_stop",
            parameters: [
                "stop_id": stopId,
            ]
        )
    }

    func tappedVehicle(routeId: String) {
        logEvent("tapped_vehicle", parameters: ["route_id": routeId])
    }
}
