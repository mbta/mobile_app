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
    func tappedAlertDetailsLegacy(routeId: String, stopId: String, alertId: String)
    func tappedRouteFilterLegacy(routeId: String, stopId: String)
}

extension AnalyticsProvider: StopDetailsAnalytics {
    func tappedAlertDetailsLegacy(routeId: String, stopId: String, alertId: String) {
        logEvent(
            "tapped_alert_details",
            parameters: [
                "route_id": routeId,
                "stop_id": stopId,
                "alertId": alertId,
            ]
        )
    }

    func tappedRouteFilterLegacy(routeId: String, stopId: String) {
        logEvent(
            "tapped_route_filter",
            parameters: [
                "route_id": routeId,
                "stop_id": stopId,
            ]
        )
    }
}
