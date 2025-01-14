//
//  StopTripDetailsAnalytics.swift
//  iosApp
//
//  Created by Horn, Melody on 2025-01-14.
//  Copyright © 2025 MBTA. All rights reserved.
//

protocol StopTripDetailsAnalytics: DestinationRowAnalytics {
    func tappedAlertDetails(routeId: String, stopId: String, alertId: String)
    func tappedRouteFilter(routeId: String, stopId: String)
    func toggledPinnedRouteAtStop(pinned: Bool, routeId: String)
}

extension AnalyticsProvider: StopTripDetailsAnalytics {
    func tappedAlertDetails(routeId: String, stopId: String, alertId: String) {
        logEvent(
            "tapped_alert_details",
            parameters: [
                "route_id": routeId,
                "stop_id": stopId,
                "alertId": alertId,
            ]
        )
    }

    func tappedRouteFilter(routeId: String, stopId: String) {
        logEvent(
            "tapped_route_filter",
            parameters: [
                "route_id": routeId,
                "stop_id": stopId,
            ]
        )
    }

    func toggledPinnedRouteAtStop(pinned: Bool, routeId: String) {
        logEvent(
            pinned ? "pin_route" : "unpin_route",
            parameters: [
                "route_id": routeId,
            ]
        )
    }
}
