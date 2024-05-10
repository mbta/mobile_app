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
    let patternsByStop: PatternsByStop?
    let now: Instant
    @Binding var filter: StopDetailsFilter?

    struct RowData {
        let tripId: String
        let headsign: String
        let formatted: PatternsByHeadsign.Format
        let navigationTarget: SheetNavigationStackEntry?

        init?(trip: UpcomingTrip, route: Route, stopId: String, expectedDirection: Int32?, now: Instant) {
            if trip.trip.directionId != expectedDirection {
                return nil
            }

            tripId = trip.trip.id
            headsign = trip.trip.headsign
            formatted = PatternsByHeadsign(
                route: route, headsign: headsign, patterns: [], upcomingTrips: [trip], alertsHere: nil
            ).format(now: now)
            if let vehicleId = trip.prediction?.vehicleId, let stopSequence = trip.stopSequence {
                navigationTarget = .tripDetails(tripId: tripId, vehicleId: vehicleId,
                                                target: .init(stopId: stopId, stopSequence: stopSequence.intValue))
            } else {
                navigationTarget = nil
            }

            if !(formatted is PatternsByHeadsign.FormatSome) {
                return nil
            }
        }
    }

    let rows: [RowData]

    init(departures: StopDetailsDepartures, now: Instant, filter filterBinding: Binding<StopDetailsFilter?>) {
        _filter = filterBinding
        let filter = filterBinding.wrappedValue
        let patternsByStop = departures.routes.first(where: { $0.route.id == filter?.routeId })
        self.patternsByStop = patternsByStop
        self.now = now

        let expectedDirection: Int32? = filter?.directionId
        if let patternsByStop {
            rows = patternsByStop.allUpcomingTrips().compactMap {
                RowData(
                    trip: $0,
                    route: patternsByStop.route,
                    stopId: patternsByStop.stop.id,
                    expectedDirection: expectedDirection,
                    now: now
                )
            }
        } else {
            rows = []
        }
    }

    var body: some View {
        content
    }

    @ViewBuilder
    var content: some View {
        if let patternsByStop {
            List {
                RoutePillSection(
                    route: patternsByStop.route,
                    headerContent: DirectionPicker(
                        patternsByStop: patternsByStop,
                        filter: $filter
                    )
                ) {
                    ForEach(rows, id: \.tripId) { row in
                        OptionalNavigationLink(value: row.navigationTarget) {
                            HeadsignRowView(headsign: row.headsign, predictions: row.formatted)
                        }
                    }
                }
            }
        } else {
            EmptyView()
        }
    }
}
