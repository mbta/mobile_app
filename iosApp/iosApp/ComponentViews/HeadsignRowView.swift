//
//  HeadsignRowView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct HeadsignRowView: View {
    let headsign: String
    let predictions: PatternsByHeadsign.Format

    var body: some View {
        HStack {
            Text(headsign)
            Spacer()
            switch onEnum(of: predictions) {
            case let .some(trips):
                ForEach(trips.trips, id: \.id) { prediction in
                    UpcomingTripView(prediction: .some(prediction.format))
                }
            case let .noService(alert):
                UpcomingTripView(prediction: .noService(alert.alert.effect))
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
            let objects = ObjectCollectionBuilder()
            let trip = objects.trip { _ in }
            let prediction = objects.prediction { prediction in
                prediction.trip = trip
                prediction.departureTime = now.addingTimeInterval(5 * 60).toKotlinInstant()
            }
            List {
                HeadsignRowView(headsign: "Some", predictions: PatternsByHeadsign.FormatSome(trips: [
                    .init(trip: .init(trip: trip, prediction: prediction), now: now.toKotlinInstant()),
                ]))
                HeadsignRowView(headsign: "None", predictions: PatternsByHeadsign.FormatNone.shared)
                HeadsignRowView(headsign: "Loading", predictions: PatternsByHeadsign.FormatLoading.shared)
                HeadsignRowView(headsign: "No Service", predictions: PatternsByHeadsign.FormatNoService(
                    alert: ObjectCollectionBuilder.Single.shared.alert { alert in
                        alert.effect = .suspension
                    }
                ))
            }
        }
    }
}
