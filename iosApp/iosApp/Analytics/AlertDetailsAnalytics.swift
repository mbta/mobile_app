//
//  AlertDetailsAnalytics.swift
//  iosApp
//
//  Created by Simon, Emma on 8/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import FirebaseAnalytics
import Foundation

protocol AlertDetailsAnalytics {
    func tappedAffectedStops(routeId: String, stopId: String, alertId: String)
    func tappedTripPlanner(routeId: String, stopId: String, alertId: String)
}

extension AnalyticsProvider: AlertDetailsAnalytics {
    func tappedAffectedStops(routeId: String, stopId: String, alertId: String) {
        logEvent(
            "tapped_affected_stops",
            parameters: [
                "route_id": routeId,
                "stop_id": stopId,
                "alert_id": alertId,
            ]
        )
    }

    func tappedTripPlanner(routeId: String, stopId: String, alertId: String) {
        logEvent(
            "tapped_trip_planner",
            parameters: [
                "route_id": routeId,
                "stop_id": stopId,
                "alert_id": alertId,
            ]
        )
    }
}
