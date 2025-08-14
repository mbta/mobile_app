//
//  RouteCardDepartures.swift
//  iosApp
//
//  Created by esimon on 4/11/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct RouteCardDepartures: View {
    var analytics: Analytics = AnalyticsProvider.shared
    let stopData: RouteCardData.RouteStopData
    let global: GlobalResponse?
    let now: EasternTimeInstant
    let isFavorite: (RouteStopDirection) -> Bool
    let pushNavEntry: (SheetNavigationStackEntry) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(stopData.data.enumerated()), id: \.element) { index, leaf in
                if let direction = stopData.directions.first(where: { $0.id == leaf.directionId }) {
                    let formatted = leaf.format(now: now, globalData: global)
                    SheetNavigationLink(
                        value: .stopDetails(
                            stopId: stopData.stop.id,
                            stopFilter: .init(
                                routeId: stopData.lineOrRoute.id,
                                directionId: leaf.directionId
                            ),
                            tripFilter: nil
                        ),
                        action: { entry in
                            print(entry)
                            pushNavEntry(entry)
                            analyticsTappedDeparture(leaf: leaf, formatted: formatted)
                        },
                        showChevron: true
                    ) {
                        RouteCardDirection(direction: direction, formatted: formatted)
                    }
                    .padding(.leading, 16)
                    .padding(.trailing, 8)
                    .padding(.vertical, 10)
                    .accessibilityHint(Text("Open for more arrivals"))
                    if index < stopData.data.count - 1 {
                        HaloSeparator()
                    }
                } else {
                    EmptyView()
                }
            }
        }
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
            routeId: stopData.lineOrRoute.id,
            stopId: stopData.stop.id,
            pinned: isFavorite(RouteStopDirection(route: stopData.lineOrRoute.id,
                                                  stop: stopData.stop.id,
                                                  direction: leaf.directionId)),
            alert: leaf.alertsHere(tripId: nil).count > 0,
            routeType: stopData.lineOrRoute.type,
            noTrips: noTrips
        )
    }
}
