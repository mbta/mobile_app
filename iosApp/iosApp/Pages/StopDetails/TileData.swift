//
//  TileData.swift
//  iosApp
//
//  Created by esimon on 11/27/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

struct TileData: Identifiable {
    let id: String
    let route: Route
    let headsign: String
    let formatted: RealtimePatterns.Format
    let upcoming: UpcomingTrip?

    init(
        route: Route,
        headsign: String,
        formatted: RealtimePatterns.Format,
        upcoming: UpcomingTrip? = nil
    ) {
        self.route = route
        self.headsign = headsign
        self.formatted = formatted
        self.upcoming = upcoming
        id = upcoming?.trip.id ?? UUID().uuidString
    }

    init?(upcoming: UpcomingTrip, route: Route, now: Instant) {
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
        self.upcoming = upcoming
        id = upcoming.trip.id
    }
}
