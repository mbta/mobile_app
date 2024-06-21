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
    let predictions: Patterns.Format
    let routeType: RouteType

    var body: some View {
        HStack(spacing: 0) {
            Text(headsign)
                .foregroundStyle(Color.text)
                .font(Typography.bodySemibold)
                .multilineTextAlignment(.leading)
            Spacer(minLength: 8)
            switch onEnum(of: predictions) {
            case let .some(trips):
                VStack(alignment: .trailing, spacing: 10) {
                    let firstTrip = trips.trips.first
                    let restTrips = trips.trips.dropFirst()

                    if let firstTrip {
                        UpcomingTripView(prediction: .some(firstTrip.format),
                                         routeType: routeType,
                                         isFirst: true,
                                         isOnly: restTrips.isEmpty)
                        ForEach(restTrips, id: \.id) { prediction in
                            UpcomingTripView(prediction: .some(prediction.format),
                                             routeType: routeType,
                                             isFirst: false,
                                             isOnly: false)
                        }
                    }
                }
                .padding(.vertical, 4)
            case let .noService(alert):
                UpcomingTripView(
                    prediction: .noService(alert.alert.effect),
                    routeType: routeType,
                    isFirst: true,
                    isOnly: true
                )
            case .none:
                UpcomingTripView(prediction: .none, routeType: routeType, isFirst: true, isOnly: true)
            case .loading:
                UpcomingTripView(prediction: .loading, routeType: routeType, isFirst: true, isOnly: true)
            }
        }
        .accessibilityInputLabels([headsign])
        .background(Color.fill3)
        .frame(maxWidth: .infinity)
    }
}

struct NearbyStopRoutePatternView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(alignment: .trailing) {
            let now = Date.now
            let objects = ObjectCollectionBuilder()
            let trip1 = objects.trip { _ in }
            let prediction1 = objects.prediction { prediction in
                prediction.trip = trip1
                prediction.departureTime = now.addingTimeInterval(5 * 60).toKotlinInstant()
            }
            let trip2 = objects.trip { _ in }
            let prediction2 = objects.prediction { prediction in
                prediction.trip = trip2
                prediction.departureTime = now.addingTimeInterval(12 * 60).toKotlinInstant()
            }
            List {
                HeadsignRowView(headsign: "Some",
                                predictions: Patterns.FormatSome(trips: [
                                    .init(
                                        trip: .init(trip: trip1, prediction: prediction1),
                                        now: now.toKotlinInstant()
                                    ),
                                    .init(
                                        trip: .init(trip: trip2, prediction: prediction2),
                                        now: now.toKotlinInstant()
                                    ),
                                ]),
                                routeType: .heavyRail)
                HeadsignRowView(headsign: "None",
                                predictions: Patterns.FormatNone.shared,
                                routeType: .heavyRail)
                HeadsignRowView(headsign: "Loading",
                                predictions: Patterns.FormatLoading.shared,
                                routeType: .heavyRail)
                HeadsignRowView(headsign: "No Service",
                                predictions: Patterns.FormatNoService(
                                    alert: ObjectCollectionBuilder.Single.shared.alert { alert in
                                        alert.effect = .suspension
                                    }
                                ),
                                routeType: .heavyRail)
            }
        }.font(Typography.body)
    }
}
