//
//  StopDetailsAnalytics.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 6/12/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import FirebaseAnalytics
import Foundation

protocol StopDetailsAnalytics {
    func tappedDepartureRow(routeId: String, stopId: String, pinned: Bool)
    func tappedRouteFilter(routeId: String, stopId: String)
}

extension AnalyticsProvider: StopDetailsAnalytics {
    func tappedDepartureRow(routeId: String, stopId: String, pinned: Bool) {
        logEvent(
            "tapped_departure",
            parameters: [
                "route_id": routeId,
                "stop_id": stopId,
                "pinned": pinned,
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
}
