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
        case noService(shared.Alert)

        static func from(upcomingTrips: [UpcomingTrip]?, alertsHere: [shared.Alert]?, now: Instant) -> Self {
            guard let upcomingTrips else { return .loading }
            let tripsToShow = upcomingTrips
                .map { TripWithFormat($0, now: now) }
                .filter { !$0.isHidden() }
                .prefix(2)
            if tripsToShow.isEmpty {
                if let alert = alertsHere?.first {
                    return .noService(alert)
                }
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
            case let .noService(alert):
                UpcomingTripView(prediction: .noService(alert.effect))
            case .none:
                UpcomingTripView(prediction: .none)
            case .loading:
                UpcomingTripView(prediction: .loading)
            }
        }
    }
}

struct NearbyStopRoutePatternView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(alignment: .trailing) {
            let now = Date.now
            NearbyStopRoutePatternView(headsign: "Some", predictions: .some([
                .init(.init(prediction: ObjectCollectionBuilder.Single.shared.prediction { prediction in
                    prediction.departureTime = now.addingTimeInterval(5 * 60).toKotlinInstant()
                }), now: now.toKotlinInstant()),
            ]))
            NearbyStopRoutePatternView(headsign: "None", predictions: .none)
            NearbyStopRoutePatternView(headsign: "Loading", predictions: .loading)
            NearbyStopRoutePatternView(headsign: "No Service", predictions: .noService(
                ObjectCollectionBuilder.Single.shared.alert { alert in
                    alert.effect = .suspension
                }
            ))
        }
    }
}
