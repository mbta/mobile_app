//
//  StopDetailsFilteredRouteView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-05.
//  Copyright © 2024 MBTA. All rights reserved.
//

import os
import shared
import SwiftUI

struct StopDetailsFilteredRouteView: View {
    var analytics: StopDetailsAnalytics = AnalyticsProvider.shared
    let patternsByStop: PatternsByStop?
    let alerts: [shared.Alert]
    let now: Instant
    @Binding var filter: StopDetailsFilter?
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let pinned: Bool

    struct RowData {
        let tripId: String
        let route: Route
        let headsign: String
        let formatted: RealtimePatterns.Format
        let navigationTarget: SheetNavigationStackEntry?

        init?(upcoming: UpcomingTrip, route: Route, stopId: String, expectedDirection: Int32?, now: Instant) {
            let trip = upcoming.trip
            if trip.directionId != expectedDirection {
                return nil
            }

            tripId = trip.id
            self.route = route
            headsign = trip.headsign
            formatted = RealtimePatterns.ByHeadsign(
                route: route, headsign: headsign, line: nil, patterns: [],
                upcomingTrips: [upcoming], alertsHere: nil
            ).format(now: now, routeType: route.type, context: .stopDetailsFiltered)

            if let vehicleId = upcoming.prediction?.vehicleId, let stopSequence = upcoming.stopSequence {
                navigationTarget = .tripDetails(tripId: tripId, vehicleId: vehicleId,
                                                target: .init(stopId: stopId, stopSequence: stopSequence.intValue),
                                                routeId: upcoming.trip.routeId, directionId: upcoming.trip.directionId)
            } else {
                navigationTarget = nil
            }

            if !(formatted is RealtimePatterns.FormatSome) {
                return nil
            }
        }
    }

    let rows: [RowData]

    init(
        departures: StopDetailsDepartures,
        global: GlobalResponse?,
        now: Instant,
        filter filterBinding: Binding<StopDetailsFilter?>,
        pushNavEntry: @escaping (SheetNavigationStackEntry) -> Void,
        pinned: Bool
    ) {
        _filter = filterBinding
        let filter = filterBinding.wrappedValue
        let patternsByStop = departures.routes.first(where: { $0.routeIdentifier == filter?.routeId })
        self.patternsByStop = patternsByStop
        self.now = now
        self.pushNavEntry = pushNavEntry
        self.pinned = pinned
        let expectedDirection: Int32? = filter?.directionId

        if let patternsByStop {
            if let expectedDirection, let global {
                alerts = patternsByStop.alertsHereFor(directionId: expectedDirection, global: global)
            } else {
                alerts = []
            }

            rows = patternsByStop.allUpcomingTrips().compactMap { upcoming in
                guard let route = (patternsByStop.routes.first { $0.id == upcoming.trip.routeId }) else {
                    Logger().error("""
                    Failed to find route ID \(upcoming.trip.routeId) from upcoming \
                    trip in patternsByStop.routes (\(patternsByStop.routes.map(\.id)))
                    """)
                    return nil
                }
                return RowData(
                    upcoming: upcoming,
                    route: route,
                    stopId: patternsByStop.stop.id,
                    expectedDirection: expectedDirection,
                    now: now
                )
            }
        } else {
            alerts = []
            rows = []
        }
    }

    var body: some View {
        if let patternsByStop {
            let routeHex: String? = patternsByStop.line?.color ?? patternsByStop.representativeRoute.color
            let routeColor: Color? = if let routeHex { Color(hex: routeHex) } else { nil }
            ZStack {
                if let routeColor {
                    routeColor
                }
                ScrollView {
                    VStack {
                        if let line = patternsByStop.line {
                            LineHeader(line: line, routes: patternsByStop.routes)
                        } else {
                            RouteHeader(route: patternsByStop.representativeRoute)
                        }
                        DirectionPicker(
                            patternsByStop: patternsByStop,
                            filter: $filter
                        ).fixedSize(horizontal: false, vertical: true)

                        ZStack {
                            Color.fill3.ignoresSafeArea(.all)
                            VStack(spacing: 0) {
                                ForEach(Array(alerts.enumerated()), id: \.offset) { index, alert in
                                    VStack(spacing: 0) {
                                        StopDetailsAlertHeader(alert: alert, routeColor: routeColor)
                                            .onTapGesture {
                                                pushNavEntry(.alertDetails(
                                                    alertId: alert.id,
                                                    line: patternsByStop.line,
                                                    routes: patternsByStop.routes
                                                ))
                                                analytics.tappedAlertDetails(
                                                    routeId: patternsByStop.routeIdentifier,
                                                    stopId: patternsByStop.stop.id,
                                                    alertId: alert.id
                                                )
                                            }
                                        if index < alerts.count - 1 || !rows.isEmpty {
                                            Divider().background(Color.halo)
                                        }
                                    }
                                }
                                ForEach(Array(rows.enumerated()), id: \.element.tripId) { index, row in
                                    VStack(spacing: 0) {
                                        OptionalNavigationLink(value: row.navigationTarget, action: { entry in
                                            pushNavEntry(entry)
                                            analytics.tappedDepartureRow(
                                                routeId: patternsByStop.routeIdentifier,
                                                stopId: patternsByStop.stop.id,
                                                pinned: pinned,
                                                alert: alerts.count > 0
                                            )
                                        }) {
                                            HeadsignRowView(
                                                headsign: row.headsign,
                                                predictions: row.formatted,
                                                pillDecoration: patternsByStop.line != nil ?
                                                    .onRow(route: row.route) : .none
                                            )
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
