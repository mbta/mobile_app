//
//  DestinationRowView.swift
//  iosApp
//
//  Created by Simon, Emma on 6/26/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct DestinationRowView: View {
    let patterns: RealtimePatterns
    let stop: Stop
    let routeId: String
    let condenseHeadsignPredictions: Bool
    let now: Instant
    let context: TripInstantDisplay.Context
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let analytics: Analytics
    let pinned: Bool
    let routeType: RouteType

    init(
        patterns: RealtimePatterns,
        stop: Stop,
        routeId: String,
        now: Instant,
        context: TripInstantDisplay.Context,
        condenseHeadsignPredictions: Bool = false,
        pushNavEntry: @escaping (SheetNavigationStackEntry) -> Void,
        analytics: Analytics,
        pinned: Bool,
        routeType: RouteType
    ) {
        self.patterns = patterns
        self.stop = stop
        self.routeId = routeId
        self.now = now
        self.context = context
        self.condenseHeadsignPredictions = condenseHeadsignPredictions
        self.pushNavEntry = pushNavEntry
        self.analytics = analytics
        self.pinned = pinned
        self.routeType = routeType
    }

    var body: some View {
        switch onEnum(of: patterns) {
        case let .byHeadsign(patternsByHeadsign):
            let predictions = patternsByHeadsign.format(
                now: now,
                routeType: patternsByHeadsign.route.type,
                count: condenseHeadsignPredictions ? 1 : 2,
                context: context
            )
            let showChevron = switch onEnum(of: predictions) {
            case .disruption: false
            default: true
            }
            SheetNavigationLink(
                value: .legacyStopDetails(
                    stop,
                    .init(
                        routeId: routeId,
                        directionId: patternsByHeadsign.directionId()
                    )
                ),
                action: { entry in
                    pushNavEntry(entry)
                    analyticsTappedDeparture(predictions: predictions)
                },
                showChevron: showChevron
            ) {
                HeadsignRowView(
                    headsign: patternsByHeadsign.headsign,
                    predictions: predictions,
                    pillDecoration: patternsByHeadsign.line != nil ?
                        .onRow(route: patternsByHeadsign.route) : .none
                )
            }
        case let .byDirection(patternsByDirection):
            let predictions = patternsByDirection.format(
                now: now,
                routeType: patternsByDirection.representativeRoute.type,
                context: context
            )
            let showChevron = switch onEnum(of: predictions) {
            case .disruption: false
            default: true
            }
            SheetNavigationLink(
                value: .legacyStopDetails(
                    stop,
                    .init(
                        routeId: routeId,
                        directionId: patternsByDirection.directionId()
                    )
                ),
                action: { entry in
                    pushNavEntry(entry)
                    analyticsTappedDeparture(predictions: predictions)
                },
                showChevron: showChevron
            ) {
                DirectionRowView(
                    direction: patternsByDirection.direction,
                    predictions: predictions,
                    pillDecoration: .onPrediction(routesByTrip: patternsByDirection.routesByTrip)
                )
            }
        }
    }

    private func analyticsTappedDeparture(predictions: RealtimePatterns.Format) {
        let noTrips: RealtimePatterns.NoTripsFormat? = switch onEnum(of: predictions) {
        case let .noTrips(noTrips): noTrips.noTripsFormat
        default: nil
        }
        analytics.tappedDeparture(
            routeId: routeId,
            stopId: stop.id,
            pinned: pinned,
            alert: (patterns.alertsHere?.count ?? 0) > 0,
            routeType: routeType,
            noTrips: noTrips
        )
    }
}
