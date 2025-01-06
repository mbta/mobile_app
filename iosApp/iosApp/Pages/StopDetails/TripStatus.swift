//
//  TripStatus.swift
//  iosApp
//
//  Created by esimon on 11/26/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct TripStatus: View {
    let predictions: RealtimePatterns.Format

    var body: some View {
        switch onEnum(of: predictions) {
        case let .some(trips):
            if let trip = trips.trips.first {
                UpcomingTripView(
                    prediction: .some(trip.format)
                )
            } else {
                EmptyView()
            }
        case let .disruption(alert):
            UpcomingTripView(prediction: .disruption(alert.alert.effect))
        case let .noTrips(format):
            UpcomingTripView(prediction: .noTrips(format.noTripsFormat))
        case .loading:
            UpcomingTripView(prediction: .loading)
        }
    }
}
