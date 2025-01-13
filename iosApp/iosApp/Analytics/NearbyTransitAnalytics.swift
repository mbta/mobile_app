//
//  NearbyTransitAnalytics.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 6/12/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import FirebaseAnalytics
import Foundation

protocol NearbyTransitAnalytics: DestinationRowAnalytics {
    func toggledPinnedRoute(pinned: Bool, routeId: String)
    func refetchedNearbyTransit()
    func tappedOnStop(stopId: String)
}

extension AnalyticsProvider: NearbyTransitAnalytics {
    func toggledPinnedRoute(pinned: Bool, routeId: String) {
        logEvent(
            pinned ? "pin_route" : "unpin_route",
            parameters: [
                "route_id": routeId,
            ]
        )
    }

    func refetchedNearbyTransit() {
        logEvent("refetched_nearby_transit")
    }

    func tappedOnStop(stopId: String) {
        logEvent(
            "tapped_on_stop",
            parameters: [
                "stop_id": stopId,
            ]
        )
    }
}
