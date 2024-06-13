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
    var analytics: StopDetailsAnalytics = AnalyticsProvider()
    let patternsByStop: PatternsByStop?
    let now: Instant
    @Binding var filter: StopDetailsFilter?
    let pushNavEntry: (SheetNavigationStackEntry) -> Void

    struct RowData {
        let tripId: String
        let headsign: String
        let formatted: PatternsByHeadsign.Format
        let navigationTarget: SheetNavigationStackEntry?

        init?(upcoming: UpcomingTrip, route: Route, stopId: String, expectedDirection: Int32?, now: Instant) {
            let trip = upcoming.trip
            if trip.directionId != expectedDirection {
                return nil
            }

            tripId = trip.id
            headsign = trip.headsign
            formatted = PatternsByHeadsign(
                route: route, headsign: headsign, patterns: [], upcomingTrips: [upcoming], alertsHere: nil
            ).format(now: now)

            if let vehicleId = upcoming.prediction?.vehicleId, let stopSequence = upcoming.stopSequence {
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

    init(departures: StopDetailsDepartures, now: Instant, filter filterBinding: Binding<StopDetailsFilter?>,
         pushNavEntry: @escaping (SheetNavigationStackEntry) -> Void) {
        _filter = filterBinding
        let filter = filterBinding.wrappedValue
        let patternsByStop = departures.routes.first(where: { $0.route.id == filter?.routeId })
        self.patternsByStop = patternsByStop
        self.now = now
        self.pushNavEntry = pushNavEntry

        let expectedDirection: Int32? = filter?.directionId
        if let patternsByStop {
            rows = patternsByStop.allUpcomingTrips().compactMap {
                RowData(
                    upcoming: $0,
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
                        OptionalNavigationLink(
                            value: row.navigationTarget,
                            action: { entry in
                                pushNavEntry(entry)
                                analytics.tappedDepartureRow(routeId: patternsByStop.route.id, stopId: patternsByStop.stop.id)
                            }
                        ) {
                            HeadsignRowView(headsign: row.headsign, predictions: row.formatted,
                                            routeType: patternsByStop.route.type)
                        }
                        .listRowBackground(Color.fill3)
                    }
                }
            }
        } else {
            EmptyView()
        }
    }
}
