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
        case let .noService(alert):
            UpcomingTripView(prediction: .noService(alert.alert.effect))
        case .none:
            UpcomingTripView(prediction: .none)
        case .noSchedulesToday:
            UpcomingTripView(prediction: .noSchedulesToday)
        case .serviceEndedToday:
            UpcomingTripView(prediction: .serviceEndedToday)
        case .loading:
            UpcomingTripView(prediction: .loading)
        }
    }
}
