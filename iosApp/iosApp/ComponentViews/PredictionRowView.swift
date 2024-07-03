//
//  PredictionRowView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct PredictionRowView: View {
    enum PillDecoration {
        case none
        case onRow(route: Route)
        case onPrediction(routesByTrip: [String: Route])
    }

    let predictions: RealtimePatterns.Format
    let routeType: RouteType
    let pillDecoration: PillDecoration
    let destination: () -> any View

    init(
        predictions: RealtimePatterns.Format,
        routeType: RouteType,
        pillDecoration: PillDecoration = .none,
        destination: @escaping () -> any View
    ) {
        self.predictions = predictions
        self.routeType = routeType
        self.pillDecoration = pillDecoration
        self.destination = destination
    }

    var body: some View {
        HStack(spacing: 0) {
            if case let .onRow(route) = pillDecoration {
                RoutePill(route: route, type: .flex).padding(.trailing, 8)
            }
            AnyView(destination())
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
        .background(Color.fill3)
        .frame(maxWidth: .infinity)
    }

    struct TripPill: View {
        let tripId: String
        let pillDecoration: PillDecoration

        var body: some View {
            if case let .onPrediction(routesByTrip) = pillDecoration, let route = routesByTrip[tripId] {
                RoutePill(route: route, type: .flex).scaleEffect(0.75).padding(.leading, 2)
            } else {
                EmptyView()
            }
        }
    }
}
