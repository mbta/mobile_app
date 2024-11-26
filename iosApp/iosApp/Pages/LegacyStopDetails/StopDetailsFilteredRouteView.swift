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
    let downstreamAlerts: [shared.Alert]
    let now: Instant
    var filter: StopDetailsFilter?
    var setFilter: (StopDetailsFilter?) -> Void
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let pinned: Bool

    struct RowData {
        let route: Route
        let headsign: String
        let formatted: RealtimePatterns.Format
        let navigationTarget: SheetNavigationStackEntry?

        init(
            route: Route,
            headsign: String,
            formatted: RealtimePatterns.Format,
            navigationTarget: SheetNavigationStackEntry? = nil
        ) {
            self.route = route
            self.headsign = headsign
            self.formatted = formatted
            self.navigationTarget = navigationTarget
        }

        init?(upcoming: UpcomingTrip, route: Route, stopId: String, now: Instant) {
            let formatted = if let formattedUpcomingTrip = RealtimePatterns.companion.formatUpcomingTrip(
                now: now,
                upcomingTrip: upcoming,
                routeType: route.type,
                context: .stopDetailsFiltered
            ) {
                RealtimePatterns.FormatSome(trips: [formattedUpcomingTrip], secondaryAlert: nil)
            } else {
                RealtimePatterns.FormatNone(secondaryAlert: nil)
            }

            if !(formatted is RealtimePatterns.FormatSome) {
                return nil
            }

            self.route = route
            headsign = upcoming.trip.headsign
            self.formatted = formatted

            if let vehicleId = upcoming.prediction?.vehicleId, let stopSequence = upcoming.stopSequence {
                navigationTarget = .tripDetails(
                    tripId: upcoming.trip.id,
                    vehicleId: vehicleId,
                    target: .init(stopId: stopId, stopSequence: stopSequence.intValue),
                    routeId: upcoming.trip.routeId,
                    directionId: upcoming.trip.directionId
                )
            } else {
                navigationTarget = nil
            }
        }
    }

    let rows: [RowData]

    init(
        departures: StopDetailsDepartures,
        global: GlobalResponse?,
        now: Instant,
        filter: StopDetailsFilter?,
        setFilter: @escaping (StopDetailsFilter?) -> Void,
        pushNavEntry: @escaping (SheetNavigationStackEntry) -> Void,
        pinned: Bool
    ) {
        self.filter = filter
        self.setFilter = setFilter
        let patternsByStop = departures.routes.first(where: { $0.routeIdentifier == filter?.routeId })
        self.patternsByStop = patternsByStop
        self.now = now
        self.pushNavEntry = pushNavEntry
        self.pinned = pinned
        let expectedDirection: Int32? = filter?.directionId

        if let patternsByStop, let expectedDirection {
            if let global {
                alerts = patternsByStop.alertsHereFor(directionId: expectedDirection, global: global)
                downstreamAlerts = patternsByStop.alertsDownstream(directionId: expectedDirection)
            } else {
                alerts = []
                downstreamAlerts = []
            }

            var rows: [RowData] = departures.stopDetailsFormattedTrips(
                routeId: patternsByStop.routeIdentifier,
                directionId: expectedDirection,
                filterAtTime: now
            ).compactMap { tripAndFormat in
                let upcoming = tripAndFormat.upcoming
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
                    now: now
                )
            }
            let realtimePatterns = patternsByStop.patterns.filter { $0.directionId() == expectedDirection }
            let statusRows = Self.getStatusDepartures(realtimePatterns: realtimePatterns, now: now)
            rows.append(contentsOf: statusRows)
            self.rows = rows
        } else {
            alerts = []
            downstreamAlerts = []
            rows = []
        }
    }

    static func getStatusDepartures(realtimePatterns: [RealtimePatterns], now: Instant) -> [RowData] {
        StopDetailsDepartures.companion.getStatusDepartures(realtimePatterns: realtimePatterns, now: now)
            .map {
                RowData(
                    route: $0.route,
                    headsign: $0.headsign,
                    formatted: $0.formatted
                )
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
                            filter: filter,
                            setFilter: setFilter
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
                                ForEach(Array(downstreamAlerts.enumerated()), id: \.offset) { index, alert in
                                    VStack(spacing: 0) {
                                        StopDetailsDownstreamAlert(alert: alert, routeColor: routeColor)
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
                                ForEach(Array(rows.enumerated()), id: \.offset) { index, row in
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
                                        .accessibilityInputLabels([row.headsign])
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
                    .padding([.bottom], 48)
                }
            }.ignoresSafeArea(.all)
        } else {
            EmptyView()
        }
    }
}
