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

struct StopDetailsFilteredRouteView: View {
    let patternsByStop: PatternsByStop
    let now: Instant
    @Binding var filter: StopDetailsFilter?

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

        let expectedDirection: Int32? = filter?.directionId
        rows = patternsByStop.allUpcomingTrips().compactMap {
            RowData(trip: $0, route: patternsByStop.route, expectedDirection: expectedDirection, now: now)
        }
    }

    var body: some View {
        List {
            RoutePillSection(route: patternsByStop.route) {
                ForEach(rows, id: \.tripId) { row in
                    HeadsignRowView(headsign: row.headsign, predictions: row.formatted)
                }
            }
        }
    }
}
