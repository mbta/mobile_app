//
//  TripDetailsAnalytics.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-08-14.
//  Copyright © 2024 MBTA. All rights reserved.
//

import FirebaseAnalytics
import Foundation

protocol TripDetailsAnalytics {
    func tappedDownstreamStop(routeId: String, stopId: String, tripId: String, connectingRouteId: String?)
}

extension AnalyticsProvider: TripDetailsAnalytics {
    func tappedDownstreamStop(routeId: String, stopId: String, tripId: String, connectingRouteId: String?) {
        logEvent("tapped_downstream_stop", parameters: [
            "route_id": routeId,
            "stop_id": stopId,
            "trip_id": tripId,
            "connecting_route_id": connectingRouteId ?? "",
        ])
    }
}
