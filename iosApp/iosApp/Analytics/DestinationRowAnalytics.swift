//
//  DestinationRowAnalytics.swift
//  iosApp
//
//  Created by Horn, Melody on 2025-01-13.
//  Copyright © 2025 MBTA. All rights reserved.
//

import FirebaseAnalytics
import shared

protocol DestinationRowAnalytics {
    func tappedDeparture(
        routeId: String,
        stopId: String,
        pinned: Bool,
        alert: Bool,
        routeType: RouteType,
        noTrips: RealtimePatterns.NoTripsFormat?
    )
}

extension AnalyticsProvider: DestinationRowAnalytics {
    func tappedDeparture(
        routeId: String,
        stopId: String,
        pinned: Bool,
        alert: Bool,
        routeType: RouteType,
        noTrips: RealtimePatterns.NoTripsFormat?
    ) {
        let mode = switch routeType {
        case .bus: "bus"
        case .commuterRail: "commuter rail"
        case .ferry: "ferry"
        case .heavyRail: "subway"
        case .lightRail: "subway"
        }
        let noTrips = switch onEnum(of: noTrips) {
        case .noSchedulesToday: "no service today"
        case .predictionsUnavailable: "predictions unavailable"
        case .serviceEndedToday: "service ended"
        case nil: ""
        }
        logEvent(
            "tapped_departure",
            parameters: [
                "route_id": routeId,
                "stop_id": stopId,
                "pinned": pinned ? "true" : "false",
                "alert": alert ? "true" : "false",
                "mode": mode,
                "no_trips": noTrips,
            ]
        )
    }
}
