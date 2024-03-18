//
//  NearbyStopRoutePatternView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct NearbyStopRoutePatternView: View {
    let headsign: String
    let predictions: PredictionState

    struct TripWithFormat: Identifiable {
        let trip: UpcomingTrip
        let format: UpcomingTrip.Format

        var id: String { trip.id }

        init(_ trip: UpcomingTrip, now: Instant) {
            self.trip = trip
            format = trip.format(now: now)
        }

        func isHidden() -> Bool {
            format is UpcomingTrip.FormatHidden
        }
    }

    enum PredictionState {
        case loading
        case none
        case some([TripWithFormat])

        static func from(upcomingTrips: [UpcomingTrip]?, now: Instant) -> Self {
            guard let upcomingTrips else { return .loading }
            let tripsToShow = upcomingTrips
                .map { TripWithFormat($0, now: now) }
                .filter { !$0.isHidden() }
                .prefix(2)
            if tripsToShow.isEmpty {
                return .none
            }
            return .some(Array(tripsToShow))
        }
    }

    var body: some View {
        HStack {
            Text(headsign)
            Spacer()
            switch predictions {
            case let .some(predictions):
                ForEach(predictions) { prediction in
                    UpcomingTripView(prediction: .some(prediction.format))
                }
            case .none:
                UpcomingTripView(prediction: .none)
            case .loading:
                UpcomingTripView(prediction: .loading)
            }
        }
    }
}
