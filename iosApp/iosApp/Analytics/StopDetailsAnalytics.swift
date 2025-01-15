//
//  StopDetailsAnalytics.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 6/12/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import FirebaseAnalytics
import Foundation

protocol StopDetailsAnalytics: DestinationRowAnalytics {
    func tappedAlertDetails(routeId: String, stopId: String, alertId: String)
    func tappedRouteFilter(routeId: String, stopId: String)
    func toggledPinnedRouteAtStop(pinned: Bool, routeId: String)
}

extension AnalyticsProvider: StopDetailsAnalytics {
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
