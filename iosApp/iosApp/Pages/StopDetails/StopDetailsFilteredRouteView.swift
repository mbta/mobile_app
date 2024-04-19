//
//  StopDetailsFilteredRouteView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-05.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

struct StopDirection {
    let name: String
    let directionId: Int
}

struct StopDetailsFilteredRouteView: View {
    let patternsByStop: PatternsByStop
    let now: Instant
    @Binding var filter: StopDetailsFilter?

    let availableDirections: [Int32]
    let destinationLabels: [String]
    let directionLabels: [String]

    struct RowData {
        let tripId: String
        let headsign: String
        let formatted: PatternsByHeadsign.Format

        init?(trip: UpcomingTrip, route: Route, expectedDirection: Int32?, now: Instant) {
            if trip.trip.directionId != expectedDirection {
                return nil
            }

            tripId = trip.trip.id
            headsign = trip.trip.headsign
            formatted = PatternsByHeadsign(
                route: route, headsign: headsign, patterns: [], upcomingTrips: [trip], alertsHere: nil
            ).format(now: now)

            if !(formatted is PatternsByHeadsign.FormatSome) {
                return nil
            }
        }
    }

    let rows: [RowData]

    init(departures: StopDetailsDepartures, now: Instant, filter filterBinding: Binding<StopDetailsFilter?>) {
        _filter = filterBinding
        let filter = filterBinding.wrappedValue
        let patternsByStop = departures.routes.first(where: { $0.route.id == filter?.routeId })!
        self.patternsByStop = patternsByStop
        self.now = now

        let route = patternsByStop.route
        let expectedDirection: Int32? = filter?.directionId
        rows = patternsByStop.allUpcomingTrips().compactMap {
            RowData(trip: $0, route: route, expectedDirection: expectedDirection, now: now)
        }

        availableDirections = Set(patternsByStop.patternsByHeadsign.map { pattern in pattern.directionId() }).sorted()
        destinationLabels = route.directionDestinations.map { $0 as? String ?? "" }
        directionLabels = route.directionNames.map { ($0 as? String ?? "").uppercased() }
    }

    var body: some View {
        List {
            RoutePillSection(route: patternsByStop.route, directionPicker: directionPicker) {
                ForEach(rows, id: \.tripId) { row in
                    HeadsignRowView(headsign: row.headsign, predictions: row.formatted)
                }
            }
        }
    }

    @ViewBuilder
    private var directionPicker: some View {
        if availableDirections.count > 1 {
            HStack(alignment: .center) {
                ForEach(availableDirections, id: \.hashValue) { direction in
                    let route: Route = patternsByStop.route
                    let action = { $filter.wrappedValue = .init(routeId: route.id, directionId: direction) }

                    Button(action: action) {
                        VStack(alignment: .leading) {
                            Text("\(directionLabels[Int(direction)]) to")
                                .font(.footnote)
                                .textCase(.none)
                            Text(destinationLabels[Int(direction)])
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .textCase(.none)
                        }
                        .padding(8)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    }
                    .background(filter?.directionId == direction ? Color(hex: route.color) : .clear)
                    .foregroundStyle(filter?.directionId == direction ? Color(hex: route.textColor) : .black)
                    .clipShape(.rect(cornerRadius: 10))
                }
            }
            .padding(3)
            .background(.white)
            .clipShape(.rect(cornerRadius: 10))
            .padding(.horizontal, -20)
        }
    }
}
