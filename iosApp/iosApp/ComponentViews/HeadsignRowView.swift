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
    enum PillDecoration {
        case none
        case onRow(route: Route)
        case onPrediction(routesByTrip: [String: Route])
    }

    var headsign: String? = nil
    var direction: Direction? = nil
    let predictions: RealtimePatterns.Format
    let routeType: RouteType
    let pillDecoration: PillDecoration

    init(
        headsign: String,
        predictions: RealtimePatterns.Format,
        routeType: RouteType,
        pillDecoration: PillDecoration = .none
    ) {
        self.headsign = headsign
        direction = nil
        self.predictions = predictions
        self.routeType = routeType
        self.pillDecoration = pillDecoration
    }

    var body: some View {
        HStack(spacing: 0) {
            if case let .onRow(route) = pillDecoration {
                RoutePill(route: route, type: .flex).padding(.trailing, 8)
            }
            destinationLabel
            Spacer(minLength: 8)
            switch onEnum(of: predictions) {
            case let .some(trips):
                VStack(alignment: .trailing, spacing: 10) {
                    ForEach(Array(trips.trips.enumerated()), id: \.1.id) { index, trip in
                        HStack(spacing: 0) {
                            UpcomingTripView(
                                prediction: .some(trip.format),
                                routeType: routeType,
                                isFirst: index == 0,
                                isOnly: index == 0 && trips.trips.count == 1
                            )
                            TripPill(tripId: trip.id, pillDecoration: pillDecoration)
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
        .accessibilityInputLabels([headsign ?? direction?.destination ?? "Unknown Destination"])
        .background(Color.fill3)
        .frame(maxWidth: .infinity)
    }

    @ViewBuilder
    var destinationLabel: some View {
        EmptyView()
        if let headsign {
            Text(headsign)
                .foregroundStyle(Color.text)
                .font(Typography.bodySemibold)
                .multilineTextAlignment(.leading)
        }
        if let direction {
            DirectionLabel(direction: direction)
                .foregroundStyle(Color.text)
        }
    }

    struct TripPill: View {
        let tripId: String
        let pillDecoration: PillDecoration

        var body: some View {
            guard case let .onPrediction(routesByTrip) = pillDecoration else {
                return AnyView(EmptyView())
            }

            guard let route = routesByTrip[tripId] else {
                return AnyView(EmptyView())
            }

            return AnyView(RoutePill(route: route, type: .flex).scaleEffect(0.75).padding(.leading, 2))
        }
    }
}

extension HeadsignRowView {
    init(
        direction: Direction,
        predictions: RealtimePatterns.Format,
        routeType: RouteType,
        pillDecoration: PillDecoration = .none
    ) {
        headsign = nil
        self.direction = direction
        self.predictions = predictions
        self.routeType = routeType
        self.pillDecoration = pillDecoration
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
                                predictions: RealtimePatterns.FormatSome(trips: [
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
                                predictions: RealtimePatterns.FormatNone.shared,
                                routeType: .heavyRail)
                HeadsignRowView(headsign: "Loading",
                                predictions: RealtimePatterns.FormatLoading.shared,
                                routeType: .heavyRail)
                HeadsignRowView(headsign: "No Service",
                                predictions: RealtimePatterns.FormatNoService(
                                    alert: ObjectCollectionBuilder.Single.shared.alert { alert in
                                        alert.effect = .suspension
                                    }
                                ),
                                routeType: .heavyRail)
            }
        }.font(Typography.body)
    }
}
