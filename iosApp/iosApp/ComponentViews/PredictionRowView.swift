//
//  PredictionRowView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct PredictionRowView: View {
    enum PillDecoration {
        case none
        case onRow(route: Route)
        case onDirectionDestination(route: Route)
    }

    let predictions: UpcomingFormat
    let pillDecoration: PillDecoration
    let destination: () -> any View

    init(
        predictions: UpcomingFormat,
        pillDecoration: PillDecoration = .none,
        destination: @escaping () -> any View
    ) {
        self.predictions = predictions
        self.pillDecoration = pillDecoration
        self.destination = destination
    }

    var body: some View {
        HStack(spacing: 0) {
            if let secondaryAlert = predictions.secondaryAlert {
                Image(secondaryAlert.iconName)
                    .accessibilityLabel("Alert")
                    .frame(width: 18, height: 18)
                    .padding(.trailing, 8)
            }
            if case let .onRow(route) = pillDecoration {
                RoutePill(route: route, type: .flex).padding(.trailing, 8)
            }
            AnyView(destination())
            Spacer(minLength: 8)
            statuses.foregroundStyle(Color.text)
        }
        .background(Color.fill3)
        .frame(maxWidth: .infinity, minHeight: 24)
    }

    @ViewBuilder
    var statuses: some View {
        switch onEnum(of: predictions) {
        case let .some(trips):
            VStack(alignment: .trailing, spacing: 10) {
                ForEach(Array(trips.trips.enumerated()), id: \.1.id) { index, trip in
                    UpcomingTripView(
                        prediction: .some(trip.format),
                        routeType: trip.routeType,
                        isFirst: index == 0,
                        isOnly: index == 0 && trips.trips.count == 1
                    )
                }
            }
        case let .disruption(alert):
            UpcomingTripView(
                prediction: .disruption(.init(alert: alert.alert), iconName: alert.iconName),
                isFirst: true,
                isOnly: true
            )
        case let .noTrips(format):
            UpcomingTripView(prediction: .noTrips(format.noTripsFormat), isFirst: true, isOnly: true)
        case .loading:
            UpcomingTripView(prediction: .loading, isFirst: true, isOnly: true)
        }
    }
}

#Preview {
    let objects = ObjectCollectionBuilder()
    let trip1 = UpcomingTrip(trip: objects.trip { _ in })
    let trip2 = UpcomingTrip(trip: objects.trip { _ in })
    VStack(alignment: .leading, spacing: 20) {
        PredictionRowView(
            predictions: .Some(trips: [
                .init(
                    trip: trip1,
                    routeType: .commuterRail,
                    format: .ScheduleTimeWithStatusRow(
                        scheduledTime: .init(year: 2025, month: .august, day: 5, hour: 12, minute: 10, second: 0),
                        status: "Delayed",
                    ),
                    lastTrip: false,
                ),
                .init(
                    trip: trip2,
                    routeType: .commuterRail,
                    format: .Time(
                        predictionTime: .init(year: 2025, month: .august, day: 5, hour: 12, minute: 45, second: 0),
                        last: false,
                        headline: true,
                    ),
                    lastTrip: false,
                ),
            ], secondaryAlert: nil),
            destination: { Text("Needham Heights") }
        )

        PredictionRowView(
            predictions: .Some(
                trip: .init(trip: trip1, routeType: .lightRail, format: .Boarding(last: false), lastTrip: false),
                secondaryAlert: nil
            ),
            destination: { Text("Longer Destination than That") }
        )

        PredictionRowView(
            predictions: .Some(trip: .init(
                trip: trip1,
                routeType: .lightRail,
                format: .Overridden(text: "Stopped 10 stops away", last: false),
                lastTrip: false,
            ), secondaryAlert: nil),
            pillDecoration: .onRow(route: TestData.getRoute(id: "Green-B")),
            destination: { Text("Destination") }
        )

        PredictionRowView(
            predictions: .Some(trip: .init(
                trip: trip1,
                routeType: .lightRail,
                format: .Overridden(text: "Stopped 10 stops away", last: false),
                lastTrip: false,
            ), secondaryAlert: nil),
            destination: { Text("Destination") }
        )

        PredictionRowView(
            predictions: .Some(trips: [
                .init(trip: trip1, routeType: .bus, format: .ScheduleMinutes(minutes: 6, last: false), lastTrip: false),
                .init(
                    trip: trip2,
                    routeType: .bus,
                    format: .ScheduleMinutes(minutes: 15, last: false),
                    lastTrip: false
                ),
            ], secondaryAlert: nil),
            destination: { Text("Destination") }
        )

        PredictionRowView(
            predictions: .Disruption(alert: objects.alert { $0.effect = .detour }, mapStopRoute: .green),
            destination: { Text("Destination") }
        )
    }
    .padding(.leading, 16)
    .padding(.trailing, 8)
}
