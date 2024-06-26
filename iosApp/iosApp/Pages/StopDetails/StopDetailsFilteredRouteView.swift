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
        let formatted: RealtimePatterns.ByHeadsign.Format
        let navigationTarget: SheetNavigationStackEntry?

        init?(upcoming: UpcomingTrip, route: Route, stopId: String, expectedDirection: Int32?, now: Instant) {
            let trip = upcoming.trip
            if trip.directionId != expectedDirection {
                return nil
            }

            tripId = trip.id
            headsign = trip.headsign
            formatted = RealtimePatterns.ByHeadsign(
                route: route, headsign: headsign, line: nil, patterns: [], upcomingTrips: [upcoming], alertsHere: nil
            ).format(now: now, count: 2)

            if let vehicleId = upcoming.prediction?.vehicleId, let stopSequence = upcoming.stopSequence {
                navigationTarget = .tripDetails(tripId: tripId, vehicleId: vehicleId,
                                                target: .init(stopId: stopId, stopSequence: stopSequence.intValue))
            } else {
                navigationTarget = nil
            }

            if !(formatted is RealtimePatterns.ByHeadsign.FormatSome) {
                return nil
            }
        }
    }

    let rows: [RowData]

    init(departures: StopDetailsDepartures, now: Instant, filter filterBinding: Binding<StopDetailsFilter?>,
         pushNavEntry: @escaping (SheetNavigationStackEntry) -> Void) {
        _filter = filterBinding
        let filter = filterBinding.wrappedValue
        let patternsByStop = departures.routes.first(where: { $0.routeIdentifier == filter?.routeId })
        self.patternsByStop = patternsByStop
        self.now = now
        self.pushNavEntry = pushNavEntry

        let expectedDirection: Int32? = filter?.directionId
        if let patternsByStop {
            rows = patternsByStop.allUpcomingTrips().compactMap {
                RowData(
                    upcoming: $0,
                    route: patternsByStop.representativeRoute,
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
        if let patternsByStop, let route = patternsByStop.routes.first {
            let routeHex: String? = route.color
            ZStack {
                if let routeHex {
                    Color(hex: routeHex)
                }
                ScrollView {
                    VStack {
                        RouteHeader(route: route)
                        DirectionPicker(
                            patternsByStop: patternsByStop,
                            filter: $filter
                        ).fixedSize(horizontal: false, vertical: true)

                        ZStack {
                            Color.fill3.ignoresSafeArea(.all)
                            VStack(spacing: 0) {
                                ForEach(Array(rows.enumerated()), id: \.element.tripId) { index, row in

                                    VStack(spacing: 0) {
                                        OptionalNavigationLink(value: row.navigationTarget, action: { entry in
                                            pushNavEntry(entry)
                                            analytics.tappedDepartureRow(
                                                routeId: route.id,
                                                stopId: patternsByStop.stop.id
                                            )
                                        }) {
                                            HeadsignRowView(headsign: row.headsign, predictions: row.formatted,
                                                            routeType: route.type)
                                        }
                                        .padding(.vertical, 10)
                                        .padding(.horizontal, 16)

                                        if index < rows.count - 1 {
                                            Divider().background(Color.halo)
                                        }
                                    }
                                }
                            }
                        }
                        .withRoundedBorder()
                    }
                    .padding([.top, .horizontal], 8)
                    .padding([.bottom], 32)
                }

            }.ignoresSafeArea(.all)

        } else {
            EmptyView()
        }
    }
}
