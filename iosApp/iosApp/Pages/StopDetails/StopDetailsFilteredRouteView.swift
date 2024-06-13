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
            let routeHex: String? = patternsByStop.route.color
            ZStack {
                // TODO: bring route color flood back here
                VStack {
                    ZStack {
                        if let routeHex {
                            Color(hex: routeHex)
                        }
                        RouteHeader(route: patternsByStop.route)
                    }.fixedSize(horizontal: false, vertical: /*@START_MENU_TOKEN@*/true/*@END_MENU_TOKEN@*/)
                    DirectionPicker(
                        patternsByStop: patternsByStop,
                        filter: $filter
                    ).fixedSize(horizontal: false, vertical: true)

                    ScrollView {
                        ZStack {
                            Color.fill3.ignoresSafeArea(.all)
                            VStack {
                                ForEach(Array(rows.enumerated()), id: \.element.tripId) { index, row in

                                    VStack(spacing: 0) {
                                        OptionalNavigationLink(value: row.navigationTarget, action: pushNavEntry) {
                                            HeadsignRowView(headsign: row.headsign, predictions: row.formatted,
                                                            routeType: patternsByStop.route.type)
                                        }
                                        .padding(8)
                                        .padding(.leading, 8)

                                        if index < rows.count - 1 {
                                            Divider().background(Color.halo)
                                        }
                                    }
                                }
                            }
                            .fixedSize(horizontal: false, vertical: true)
                        }
                        .clipShape(.rect(cornerRadius: 8))
                        .border(Color.halo.opacity(0.1), width: 1)
                    }
                }.padding([.top, .horizontal], 8)
            }.ignoresSafeArea(.all)

        } else {
            EmptyView()
        }
    }
}
