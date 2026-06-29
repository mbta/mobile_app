//
//  StopCard.swift
//  iosApp
//
//  Created by Melody Horn on 6/25/26.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct StopCardContainer<Content: View>: View {
    @ObserveInjection var inject
    let cardData: StopCardData
    let departureContent: (StopCardData) -> Content

    var body: some View {
        VStack(spacing: 0) {
            StopCardStopHeader(data: cardData)
            departureContent(cardData)
        }
        .background(Color.fill3)
        .withRoundedBorder()
        .enableInjection()
    }
}

struct StopCard: View {
    @ObserveInjection var inject
    let cardData: StopCardData
    let global: GlobalResponse?
    let now: EasternTimeInstant
    let isFavorite: (RouteStopDirection) -> Bool
    let pushNavEntry: (SheetNavigationStackEntry) -> Void

    @EnvironmentObject var settingsCache: SettingsCache
    var showStationAccessibility: Bool { settingsCache.get(.stationAccessibility) }

    @ScaledMetric private var modeIconHeight: CGFloat = 24

    var body: some View {
        StopCardContainer(cardData: cardData) { stopData in
            StopCardDepartures(
                stopData: stopData,
                global: global,
                now: now,
                isFavorite: isFavorite,
                pushNavEntry: pushNavEntry
            )
        }
        .enableInjection()
    }
}

struct StopCardDepartures: View {
    @ObserveInjection var inject
    var analytics: Analytics = AnalyticsProvider.shared
    let stopData: StopCardData
    let global: GlobalResponse?
    let now: EasternTimeInstant
    let isFavorite: (RouteStopDirection) -> Bool
    let pushNavEntry: (SheetNavigationStackEntry) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(stopData.data.enumerated()), id: \.element) { index, leaf in
                let formatted = leaf.format(now: now, globalData: global)
                SheetNavigationLink(
                    value: .stopDetails(
                        stopId: stopData.stop.id,
                        stopFilter: .init(
                            routeId: leaf.lineOrRoute.id,
                            directionId: leaf.direction.id
                        ),
                        tripFilter: nil
                    ),
                    action: { entry in
                        pushNavEntry(entry)
                        analyticsTappedDeparture(leaf: leaf, formatted: formatted)
                    },
                    showChevron: true
                ) {
                    let branchedNoRoutePills = (formatted as? LeafFormat.Branched)?.branchRows
                        .allSatisfy { $0.route == nil } ?? false
                    if branchedNoRoutePills {
                        RoutePill(
                            route: (leaf.lineOrRoute as? LineOrRoute.Route)?.route,
                            line: (leaf.lineOrRoute as? LineOrRoute.Line)?.line,
                            type: .fixed
                        )
                    }
                    if leaf.lineOrRoute.id == WorldCupService.shared.route.id {
                        WorldCupBlurb(
                            leaf: leaf,
                            routeAccents: .init(route: leaf.lineOrRoute.sortRoute),
                            offerDetails: false
                        )
                    } else {
                        StopCardDirection(
                            direction: leaf.direction,
                            formatted: formatted,
                            lineOrRoute: leaf.lineOrRoute
                        )
                    }
                }
                .tint(.fill3)
                .padding(.leading, 16)
                .padding(.trailing, 8)
                .padding(.vertical, 10)
                .accessibilityHint(Text("Open for more arrivals"))
                if index < stopData.data.count - 1 {
                    HaloSeparator()
                }
            }
        }
        .enableInjection()
    }

    private func analyticsTappedDeparture(leaf: RouteCardData.Leaf, formatted: LeafFormat) {
        let upcoming: UpcomingFormat? = switch onEnum(of: formatted) {
        case let .single(single): single.format
        default: nil
        }
        let noTrips: UpcomingFormat.NoTripsFormat? = switch onEnum(of: upcoming) {
        case let .noTrips(formatted): formatted.noTripsFormat
        default: nil
        }
        analytics.tappedDeparture(
            routeId: leaf.lineOrRoute.id,
            stopId: stopData.stop.id,
            pinned: isFavorite(RouteStopDirection(route: leaf.lineOrRoute.id,
                                                  stop: stopData.stop.id,
                                                  direction: leaf.direction.id)),
            alert: leaf.alertsHere(tripId: nil).count > 0,
            routeType: leaf.lineOrRoute.type,
            noTrips: noTrips
        )
    }
}
